package com.example.animalApp.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.example.animalApp.R
import com.example.animalApp.backend.NetworkState
import com.example.animalApp.backend.Shared
import com.example.animalApp.databinding.FragmentPrivacyBinding

class PrivacyFragment : Fragment() {

    private lateinit var mBinding: FragmentPrivacyBinding
    val safeArgs: TermsAndCondFragmentArgs by navArgs()
    private val isOnlyRead by lazy { safeArgs.isOnlyRead }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?{
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_privacy, container, false)
        return mBinding.root
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mBinding.progressPrivacy.show()
        mBinding.toolPrivacy.setupWithNavController(
            findNavController(),
            AppBarConfiguration(findNavController().graph)
        )
        mBinding.toolPrivacy.title = getString(R.string.privacy_policy)
        if(!Shared.getInstance().getTermIsRead()){
            mBinding.toolPrivacy.setNavigationIcon(R.drawable.ic_close)
            mBinding.toolPrivacy.setNavigationOnClickListener {
                activity?.finish()
            }
        }

        mBinding.wvPrivacy.webViewClient = object: WebViewClient(){
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                mBinding.progressPrivacy.hide()
            }
        }
        val settings: WebSettings = mBinding.wvPrivacy.settings
        settings.defaultTextEncodingName = "utf-8"
        settings.javaScriptEnabled = true
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            NetworkState.getInstance().runWhenNetworkAvailable {
                mBinding.wvPrivacy.loadUrl("https://www.test/live")
            }
        }else{
            mBinding.wvPrivacy.loadUrl("https://www.test/live_new")
        }

    }


}