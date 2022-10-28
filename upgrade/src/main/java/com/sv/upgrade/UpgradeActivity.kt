package com.sv.upgrade

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.tasks.Task

class UpgradeActivity : AppCompatActivity() {

    companion object {
        private const val IN_APP_UPDATE_REQUEST_CODE = 1
    }

    private var updateType: Int = AppUpdateType.IMMEDIATE

    private var appUpdateManager: AppUpdateManager? = null

    private lateinit var installStateUpdatedListener: InstallStateUpdatedListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upgrade)

        // Creates instance of the manager.
        appUpdateManager = AppUpdateManagerFactory.create(baseContext)
    }

    override fun onResume() {
        super.onResume()
        checkAppUpdate()
    }
    /** This is needed to handle the result of the manager.startConfirmationDialogForResult request */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == IN_APP_UPDATE_REQUEST_CODE) {
            if (resultCode != RESULT_OK) {
                Toast.makeText(
                    this,
                    "Update flow failed! Result code: $resultCode",
                    Toast.LENGTH_LONG
                ).show()
                // If the update is cancelled or fails,
                // you can request to start the update again.
                checkAppUpdate()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun checkAppUpdate() {
        // Returns an intent object that you use to check for an update.
        val appUpdateInfo = appUpdateManager?.appUpdateInfo

        // Checks that the platform will allow the specified type of update.
        appUpdateInfo?.addOnSuccessListener {
            handleUpdate(appUpdateManager, appUpdateInfo)
        }
    }

    private fun handleUpdate(manager: AppUpdateManager?, info: Task<AppUpdateInfo>) {
        updateType = info.result.updateAvailability()
        when (updateType) {
            AppUpdateType.IMMEDIATE -> handleImmediateUpdate(manager, info)
            AppUpdateType.FLEXIBLE -> handleFlexibleUpdate(manager, info)
            else -> throw Exception("Unexpected error")
        }
    }

    private fun handleFlexibleUpdate(
        appUpdateManager: AppUpdateManager?,
        info: Task<AppUpdateInfo>
    ) {
        if ((info.result.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE ||
                    info.result.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) && info.result.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
        ) {
            setUpdateAction(appUpdateManager, info)
        }
    }

    private fun handleImmediateUpdate(
        appUpdateManager: AppUpdateManager?,
        info: Task<AppUpdateInfo>
    ) {
        if ((info.result.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE ||
                    info.result.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) && info.result.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
        ) {
            appUpdateManager?.startUpdateFlowForResult(
                info.result,
                AppUpdateType.IMMEDIATE, this, IN_APP_UPDATE_REQUEST_CODE
            )
        }
    }

    private fun setUpdateAction(manager: AppUpdateManager?, info: Task<AppUpdateInfo>) {
        // Before starting an update, register a listener for updates.

            installStateUpdatedListener = InstallStateUpdatedListener {
                when (it.installStatus()) {
                    InstallStatus.FAILED, InstallStatus.UNKNOWN -> {
                        popupSnackBarUpdateStates(getString(R.string.info_failed))
                    }
                    InstallStatus.PENDING -> {
                        popupSnackBarUpdateStates(getString(R.string.info_pending))
                    }
                    InstallStatus.CANCELED -> {
                        popupSnackBarUpdateStates(getString(R.string.info_canceled))
                    }
                    InstallStatus.DOWNLOADING -> {
                        popupSnackBarUpdateStates(getString(R.string.info_downloading))
                    }
                    InstallStatus.DOWNLOADED -> {
                        popupSnackBarUpdateStates(getString(R.string.info_downloaded))
                    }
                    InstallStatus.INSTALLING -> {
                        popupSnackBarUpdateStates(getString(R.string.info_installing))
                    }
                    InstallStatus.INSTALLED -> {
                        popupSnackBarUpdateStates(getString(R.string.info_installed))
                        manager?.unregisterListener(installStateUpdatedListener)
                    }
                    else -> {
                        popupSnackBarUpdateStates(getString(R.string.info_restart))
                    }
                }
            }
            manager?.registerListener(installStateUpdatedListener)
        info.result?.let {
            manager?.startUpdateFlowForResult(
                it, AppUpdateType.FLEXIBLE,
                this, IN_APP_UPDATE_REQUEST_CODE
            )
        }
    }

    /* Displays the SnackBar notification  */
    private fun popupSnackBarUpdateStates(text: String) {
        Snackbar.make(
            findViewById(R.id.container),
            text, Snackbar.LENGTH_INDEFINITE
        ).show()
    }
}