package com.alphito.kcal.core.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.alphito.kcal.core.common.dispatcher.DispatcherProvider
import com.alphito.kcal.core.common.flags.FeatureFlags
import com.alphito.kcal.core.data.repository.PreferencesRepository
import com.alphito.kcal.core.firebase.KcalFirebase
import com.alphito.kcal.core.model.PremiumPlan
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryPurchasesAsync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * Google Play Billing 9.
 *
 * With `server_verification_enabled` on (Blaze plan + deployed functions): Purchase → Cloud Function
 * `verifySubscription` (Play Developer API) → entitlement in Firestore
 * (`users/{uid}/entitlements/premium`) and acknowledgement on the server; the local plan is only
 * granted once the server confirms it. With it off (free Spark plan) the purchase is acknowledged
 * on the device and restored from Play on every launch.
 */
internal class PlayBillingRepository(
    context: Context,
    private val preferences: PreferencesRepository,
    private val firebase: KcalFirebase,
    private val featureFlags: FeatureFlags,
    private val appScope: CoroutineScope,
    private val dispatchers: DispatcherProvider,
) : BillingRepository, PurchasesUpdatedListener {

    private val packageName = context.packageName

    private val client: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().enablePrepaidPlans().build())
        .enableAutoServiceReconnection()
        .build()

    private val _offers = MutableStateFlow<Map<PremiumPlan, PlanOffer>>(emptyMap())
    override val offers: StateFlow<Map<PremiumPlan, PlanOffer>> = _offers.asStateFlow()

    private val _outcomes = MutableSharedFlow<PurchaseOutcome>(extraBufferCapacity = 4, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    override val outcomes: Flow<PurchaseOutcome> = _outcomes.asSharedFlow()

    private var productDetails: ProductDetails? = null
    /** Plan chosen in the purchase sheet – only needed by the no-server development fallback. */
    @Volatile private var lastRequestedPlan: PremiumPlan? = null
    private val connectMutex = Mutex()

    override suspend fun refresh() = withContext(dispatchers.io) {
        if (!connect()) return@withContext
        loadProducts()
        restore()
    }

    override suspend fun launchPurchase(activity: Activity, plan: PremiumPlan): Boolean {
        if (!connect()) return false.also { _outcomes.emit(PurchaseOutcome.Failed("billing_unavailable")) }
        if (productDetails == null) withContext(dispatchers.io) { loadProducts() }
        val details = productDetails ?: return false.also { _outcomes.emit(PurchaseOutcome.Failed("product_unavailable")) }
        val offer = _offers.value[plan] ?: return false.also { _outcomes.emit(PurchaseOutcome.Failed("plan_unavailable")) }
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(details).setOfferToken(offer.offerToken).build()),
            )
            .build()
        lastRequestedPlan = plan
        val result = withContext(dispatchers.main) { client.launchBillingFlow(activity, params) }
        return result.responseCode == BillingResponseCode.OK
    }

    /** Called by Play on its own thread; work is moved to the app scope. */
    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        appScope.launch(dispatchers.io) {
            when (result.responseCode) {
                BillingResponseCode.OK -> purchases.orEmpty().forEach { handle(it, userInitiated = true) }
                BillingResponseCode.USER_CANCELED -> _outcomes.emit(PurchaseOutcome.Cancelled)
                BillingResponseCode.ITEM_ALREADY_OWNED -> restore()
                else -> _outcomes.emit(PurchaseOutcome.Failed(result.debugMessage.ifBlank { "code_${result.responseCode}" }))
            }
        }
    }

    private suspend fun connect(): Boolean = connectMutex.withLock {
        if (client.isReady) return true
        suspendCancellableCoroutine { cont ->
            client.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    if (cont.isActive) cont.resume(result.responseCode == BillingResponseCode.OK)
                }
                override fun onBillingServiceDisconnected() = Unit // auto-reconnection is enabled
            })
        }
    }

    private suspend fun loadProducts() {
        val params = QueryProductDetailsParams.newBuilder().setProductList(
            listOf(
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(BillingProducts.SUBSCRIPTION_ID)
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build(),
            ),
        ).build()
        val details = suspendCancellableCoroutine<ProductDetails?> { cont ->
            client.queryProductDetailsAsync(params) { result, products ->
                if (cont.isActive) cont.resume(if (result.responseCode == BillingResponseCode.OK) products.productDetailsList.firstOrNull() else null)
            }
        } ?: return
        productDetails = details
        _offers.value = details.subscriptionOfferDetails.orEmpty()
            .filter { it.offerId == null } // base plans only; promo offers can be added later
            .mapNotNull { offer ->
                val plan = BillingProducts.planFor(offer.basePlanId) ?: return@mapNotNull null
                val phase = offer.pricingPhases.pricingPhaseList.lastOrNull() ?: return@mapNotNull null
                plan to PlanOffer(plan, phase.formattedPrice, phase.billingPeriod, offer.offerToken)
            }.toMap()
    }

    /** Re-checks owned subscriptions; clears premium when nothing active remains. */
    private suspend fun restore() {
        val result = client.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build())
        if (result.billingResult.responseCode != BillingResponseCode.OK) return
        val active = result.purchasesList.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
        if (active.isEmpty()) preferences.setPlan(null) else active.forEach { handle(it, userInitiated = false) }
    }

    private suspend fun handle(purchase: Purchase, userInitiated: Boolean) {
        when (purchase.purchaseState) {
            Purchase.PurchaseState.PENDING -> if (userInitiated) _outcomes.emit(PurchaseOutcome.Pending)
            Purchase.PurchaseState.PURCHASED -> {
                val plan = verify(purchase)
                if (plan != null) {
                    preferences.setPlan(plan)
                    if (userInitiated) _outcomes.emit(PurchaseOutcome.Active(plan))
                } else if (userInitiated) {
                    _outcomes.emit(PurchaseOutcome.Failed("verification_failed"))
                }
            }
            else -> Unit
        }
    }

    /**
     * Server verification when the `server_verification_enabled` flag is on (requires the Cloud
     * Function, i.e. the Blaze plan); otherwise the purchase is acknowledged on the device so Play
     * never auto-refunds it after 3 days.
     */
    private suspend fun verify(purchase: Purchase): PremiumPlan? {
        val functions = if (featureFlags.flags.value.serverVerificationEnabled) firebase.functions() else null
        if (functions != null && firebase.uid() != null) {
            return runCatching {
                val response = functions.getHttpsCallable("verifySubscription").call(
                    mapOf(
                        "packageName" to packageName,
                        "productId" to BillingProducts.SUBSCRIPTION_ID,
                        "purchaseToken" to purchase.purchaseToken,
                    ),
                ).await().getData() as? Map<*, *>
                if (response?.get("active") == true) BillingProducts.planFor(response["basePlanId"] as? String) else null
            }.onFailure { Log.w(TAG, "Server verification failed", it) }.getOrNull()
        }
        Log.i(TAG, "Server verification off – acknowledging on device")
        if (!purchase.isAcknowledged) {
            val ack = client.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build())
            if (ack.responseCode != BillingResponseCode.OK) return null
        }
        // A Purchase does not carry its base plan; without the server use the plan the user picked,
        // or keep the plan already recorded for this install when restoring.
        return lastRequestedPlan ?: preferences.snapshot().subscription.activePlan ?: PremiumPlan.Monthly
    }

    private companion object { const val TAG = "PlayBilling" }
}
