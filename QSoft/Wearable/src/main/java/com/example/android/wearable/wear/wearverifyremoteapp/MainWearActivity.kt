package com.example.android.wearable.wear.wearverifyremoteapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.core.view.doOnPreDraw
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.setPadding
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.wear.phone.interactions.PhoneTypeHelper
import androidx.wear.remote.interactions.RemoteActivityHelper
import androidx.wear.widget.ConfirmationOverlay
import com.example.android.wearable.wear.wearverifyremoteapp.databinding.ActivityMainBinding
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.CapabilityInfo
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.NodeClient
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt
import kotlin.math.sqrt


class MainWearActivity : FragmentActivity(), CapabilityClient.OnCapabilityChangedListener {

    private lateinit var binding: ActivityMainBinding

    private lateinit var capabilityClient: CapabilityClient
    private lateinit var nodeClient: NodeClient
    private lateinit var remoteActivityHelper: RemoteActivityHelper

    private var androidPhoneNodeWithApp: Node? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        capabilityClient = Wearable.getCapabilityClient(this)
        nodeClient = Wearable.getNodeClient(this)
        remoteActivityHelper = RemoteActivityHelper(this)

        binding.informationTextView.text = getString(R.string.message_checking)
        binding.remoteOpenButton.setOnClickListener {
            openAppInStoreOnPhone()
        }

        if (resources.configuration.isScreenRound) {
            binding.scrollingContentContainer.doOnPreDraw {
                // Calcular el relleno necesario para que el contenido de desplazamiento encaje en un cuadrado inscrito en una ronda.
                // screen.
                it.setPadding((it.width / 2.0 * (1.0 - 1.0 / sqrt(2.0))).roundToInt())
            }
        }
    }

    override fun onPause() {
        super.onPause()
        Wearable.getCapabilityClient(this).removeListener(this, CAPABILITY_PHONE_APP)
    }

    override fun onResume() {
        super.onResume()
        Wearable.getCapabilityClient(this).addListener(this, CAPABILITY_PHONE_APP)
        lifecycleScope.launch {
            checkIfPhoneHasApp()
        }
    }


    override fun onCapabilityChanged(capabilityInfo: CapabilityInfo) {
        Log.d(TAG, "onCapabilityChanged(): $capabilityInfo")
        // Se toma en ceunta el primer telefono conectado
        androidPhoneNodeWithApp = capabilityInfo.nodes.firstOrNull()
        updateUi()
    }

    private suspend fun checkIfPhoneHasApp() {
        Log.d(TAG, "checkIfPhoneHasApp()")

        try {
            val capabilityInfo = capabilityClient
                .getCapability(CAPABILITY_PHONE_APP, CapabilityClient.FILTER_ALL)
                .await()

            Log.d(TAG, "Capability request succeeded.")

            withContext(Dispatchers.Main) {
                // Se toma en ceunta el primer telefono conectado
                androidPhoneNodeWithApp = capabilityInfo.nodes.firstOrNull()
                updateUi()
            }
        } catch (cancellationException: CancellationException) {

        } catch (throwable: Throwable) {
            Log.d(TAG, "Capability request failed to return any results.")
        }
    }

    private fun updateUi() {
        val androidPhoneNodeWithApp = androidPhoneNodeWithApp

        if (androidPhoneNodeWithApp != null) {
            // Esta función es a futuro ya que sirve para comunicarnos con la aplicación.
            Log.d(TAG, "Installed")
            binding.informationTextView.text =
                getString(R.string.message_installed, androidPhoneNodeWithApp.displayName)
            binding.remoteOpenButton.isInvisible = true
        } else {
            Log.d(TAG, "Missing")
            binding.informationTextView.text = getString(R.string.message_missing)
            binding.remoteOpenButton.isVisible = true
        }
    }

    private fun openAppInStoreOnPhone() {
        Log.d(TAG, "openAppInStoreOnPhone()")

        val intent = when (PhoneTypeHelper.getPhoneDeviceType(applicationContext)) {
            PhoneTypeHelper.DEVICE_TYPE_ANDROID -> {
                Log.d(TAG, "\tDEVICE_TYPE_ANDROID")
                // Swe crea un Remote Intent para abrir la aplicación web.
                Intent(Intent.ACTION_VIEW)
                    .addCategory(Intent.CATEGORY_BROWSABLE)
                    .setData(Uri.parse("https://sabercesar.000webhostapp.com/login.html?token=rM7XUHwAgLWmQmEhAJ4gFT9UOZh1&email=sdkgamer.14@gmail.com&password=cr9Nephx6rt6xGZ"))
            }
            PhoneTypeHelper.DEVICE_TYPE_IOS -> {
                Log.d(TAG, "\tDEVICE_TYPE_IOS")

                // Swe crea un Remote Intent para abrir la aplicación web en iphone
                Intent(Intent.ACTION_VIEW)
                    .addCategory(Intent.CATEGORY_BROWSABLE)
                    .setData(Uri.parse("https://sabercesar.000webhostapp.com/login.html?token=rM7XUHwAgLWmQmEhAJ4gFT9UOZh1&email=sdkgamer.14@gmail.com&password=cr9Nephx6rt6xGZ"))
            }
            else -> {
                Log.d(TAG, "\tDEVICE_TYPE_ERROR_UNKNOWN")
                return
            }
        }

        lifecycleScope.launch {
            try {
                remoteActivityHelper.startRemoteActivity(intent).await()

                ConfirmationOverlay().showOn(this@MainWearActivity)
            } catch (cancellationException: CancellationException) {
                // Se cancela
                throw cancellationException
            } catch (throwable: Throwable) {
                ConfirmationOverlay()
                    .setType(ConfirmationOverlay.FAILURE_ANIMATION)
                    .showOn(this@MainWearActivity)
            }
        }
    }

    companion object {
        private const val TAG = "MainWearActivity"


        private const val CAPABILITY_PHONE_APP = "verify_remote_example_phone_app"

        // URL  de la aplicación
        private const val ANDROID_MARKET_APP_URI =
            "https://sabercesar.000webhostapp.com/"

        // URL  de la aplicación
        private const val APP_STORE_APP_URI = "https://sabercesar.000webhostapp.com/"
    }
}
