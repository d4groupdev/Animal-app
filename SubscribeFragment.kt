package com.example.animalApp.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.CountDownTimer
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.android.billingclient.api.*
import com.anjlab.android.iab.v3.BillingProcessor
import com.anjlab.android.iab.v3.TransactionDetails
import com.example.animalApp.BuildConfig
import com.example.animalApp.R
import com.example.animalApp.backend.Firestore
import com.example.animalApp.backend.Shared
import com.example.animalApp.databinding.FragmentSubscriptionBinding
import java.util.*


class SubscribeFragment : Fragment(), BillingProcessor.IBillingHandler {
    lateinit var billingProcessor: BillingProcessor

    private lateinit var mBinding: FragmentSubscriptionBinding
    private lateinit var billingClient: BillingClient
    private val mSkuDetailsMap: MutableMap<String, SkuDetails> = HashMap<String, SkuDetails>()
    private var sku: String = "test_year_sub"
    private lateinit var mToast: Toast
    private val toastCountDownTimer = object : CountDownTimer(5000, 1000) {
        override fun onFinish() {
            mToast.cancel()
        }

        override fun onTick(millisUntilFinished: Long) {
            mToast.show()
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mBinding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_subscription, container, false)
        return mBinding.root
    }

    @SuppressLint("HardwareIds")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        billingProcessor = BillingProcessor(requireContext(), BuildConfig.BillingKey, this)
        val androidId = Settings.Secure.getString(
            requireContext().contentResolver,
            Settings.Secure.ANDROID_ID
        )
        Log.d("Android", "ID: $androidId")
        mToast =
            Toast.makeText(requireContext(), getString(R.string.trial_is_over), Toast.LENGTH_LONG)

        moveToNextScreen()
        if (Shared.getInstance().getEnterCount() == 0) {
            // mBinding.btnSubscribeTrial.isEnabled = false
            mToast.show()
            toastCountDownTimer.start()
            mBinding.btnSubscribeTrial.text = getString(R.string.subscribe)
            initBilling()
            //billingProcessor.initialize()

        } else if (Shared.getInstance().getEnterCount() == -5) {

        }
        mBinding.btnSubscribeTrial.setOnClickListener {
            if (Shared.getInstance().getEnterCount() > 0) {
                Shared.getInstance().setEnterCount(Shared.getInstance().getEnterCount() - 1)
                val androidId = Settings.Secure.getString(
                    requireContext().contentResolver,
                    Settings.Secure.ANDROID_ID
                )
                Log.d("Android", "ID: $androidId")
                Firestore.getInstance()
                    .putUserCount(androidId, Shared.getInstance().getEnterCount())
                moveToNextScreen()
            } else {
                launchBilling(sku)
            }
        }
        mBinding.tvSubscriptionTerm.setOnClickListener {
            val action = SubscribeFragmentDirections.actionSubscribeFragmentToTermsAndCondFragment()
            action.isOnlyRead = true
            findNavController().navigate(action)
        }
    }

    private fun moveToNextScreen() {
        if (Shared.getInstance().getTermIsRead()) {
            findNavController().navigate(SubscribeFragmentDirections.actionSubscribeFragmentToChoosePhotoFragment())
        } else {
            findNavController().navigate(SubscribeFragmentDirections.actionSubscribeFragmentToTermsAndCondFragment())
        }
    }


    private fun initBilling() {
        billingClient = BillingClient.newBuilder(requireContext()).enablePendingPurchases()
            .setListener { p0, p1 ->
                if (p0.responseCode == BillingClient.BillingResponseCode.OK && p1 != null) {
                    activity?.runOnUiThread { moveToNextScreen() }
                }
                Log.d("Billing", "Resp code: ${p0.responseCode} p1: $p1")
            }.build()

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(p0: BillingResult) {
                Log.i(
                    "Billing",
                    "${p0.responseCode} ${BillingClient.BillingResponseCode.OK}"
                )
                if (p0.responseCode == BillingClient.BillingResponseCode.OK) {
                    // The BillingClient is ready. You can query purchases here.
                    Log.d("Billing", "Connect code: ${p0.responseCode}")

                    val purchasesResult =
                        billingClient.queryPurchases(BillingClient.SkuType.SUBS)
                    billingClient.queryPurchaseHistoryAsync(
                        BillingClient.SkuType.SUBS
                    ) { billingResult1: BillingResult, _: List<PurchaseHistoryRecord?>? ->
                        Log.d(
                            "billingprocess",
                            "purchasesResult.getPurchasesList():" + purchasesResult.purchasesList
                        )
                        if (billingResult1.responseCode == BillingClient.BillingResponseCode.OK &&
                            Objects.requireNonNull(purchasesResult.purchasesList)
                                ?.isNotEmpty() == true
                        ) {
                            activity?.runOnUiThread { moveToNextScreen() }
                        }
                    }
                    donate()

                }

            }

            override fun onBillingServiceDisconnected() {
                Log.i("Billing", "onBillingServiceDisconnected")
            }
        })
    }

    fun launchBilling(skuId: String?) {
        if (mSkuDetailsMap.isNotEmpty()) {
            Log.d("Billing", "SkuDetails ${mSkuDetailsMap[skuId!!]}")
            val billingFlowParams = BillingFlowParams.newBuilder()
                .setSkuDetails(mSkuDetailsMap[skuId]!!)
                .build()
            billingClient.launchBillingFlow(requireActivity(), billingFlowParams)
        }
        Log.d("Billing", "mScuDetailsMap: $mSkuDetailsMap")
    }

    fun donate() {
        val skuList = ArrayList<String>()
        skuList.add(sku)
        val params = SkuDetailsParams.newBuilder()
        params.setSkusList(skuList).setType(BillingClient.SkuType.SUBS)

        billingClient.querySkuDetailsAsync(params.build()) { p0, p1 ->
            Log.i("Billing", "${p1.toString()} +++++++++++ onSkuDetailsResponse")
            if (p0.responseCode == 0) {
                for (skuDetails in p1!!) {
                    Log.i("Billing", skuDetails.sku)
                    mSkuDetailsMap[skuDetails.sku] = skuDetails
                }
            }
        }
    }

    override fun onProductPurchased(productId: String, details: TransactionDetails?) {
        if (billingProcessor.isSubscribed(productId)) {
            val toastTrue =
                Toast.makeText(context, "Оплата выполнена успешно!", Toast.LENGTH_SHORT)
            toastTrue.show()
            moveToNextScreen()
        }
    }

    override fun onBillingError(errorCode: Int, error: Throwable?) {
        Log.e("Billing", error!!.localizedMessage!!)
    }
}