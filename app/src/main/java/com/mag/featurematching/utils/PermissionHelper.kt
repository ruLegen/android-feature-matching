package com.mag.featurematching.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.lang.ref.WeakReference

class PermissionHelper (val context:WeakReference<Activity>, val permissions:Array<String>) {
    private val PERMISSION_CODE: Int = 10101

    var permissionListener:((b:Boolean)->Unit)? = null

    val isAllGranted: Boolean
        get() = checkPermissions(context.get()!!)



    fun requestPermissions(listener:((isGranted:Boolean)->Unit)?){
        permissionListener = listener
        if(isAllGranted){
            permissionListener?.invoke(true)
            return
        }
        ActivityCompat.requestPermissions(context.get()!!,permissions,PERMISSION_CODE)
    }

    fun onRequestPermissionsResult(requestCode: Int,permissions: Array<out String>,grantResults: IntArray) {
        if(requestCode != PERMISSION_CODE)
            return
        var isAllPermissionsGranted = true
        for (res in grantResults){
            isAllPermissionsGranted = isAllPermissionsGranted && res == PackageManager.PERMISSION_GRANTED
        }
        permissionListener?.invoke(isAllPermissionsGranted)
    }

    private fun checkPermissions(context: Context) : Boolean {
        val permissionList = arrayListOf(Manifest.permission.CAMERA);
        var isAllGranted = true
        for(p in permissionList){
            val isGranted =  ContextCompat.checkSelfPermission(context,p) == PackageManager.PERMISSION_GRANTED
            isAllGranted = isAllGranted && isGranted
        }
        return isAllGranted;
    }
}