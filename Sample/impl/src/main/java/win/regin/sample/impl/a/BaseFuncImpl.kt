package win.regin.sample.impl.a

import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.app.StatusBarManager
import android.content.pm.IPackageDeleteObserver
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.util.Log
import com.orhanobut.logger.Logger
import win.regin.sample.impl.m.ContextManager

/**
 * @author :Reginer in  2020/10/14 13:58.
 * 联系方式:QQ:282921012
 * 功能描述:
 */
@SuppressLint("NewApi")
open class BaseFuncImpl : IFunction {
    var mContext = ContextManager.getInstance().context

    /**
     * 设置Home键是否可用
     *
     * @param enable false不可用
     */
    override fun setHomeEnable(enable: Boolean) {
        val statusBarManager = mContext.getSystemService(StatusBarManager::class.java)
        if (enable) {
            statusBarManager?.disable(StatusBarManager.DISABLE_NONE)
        } else {
            statusBarManager?.disable(StatusBarManager.DISABLE_HOME)
        }
    }

    /**
     * 卸载app
     *
     * @param packageName 应用包名
     * @return 这个返回值没什么软用
     */
    override fun uninstallPackage(packageName: String): Boolean {
        val observer: IPackageDeleteObserver = object : IPackageDeleteObserver.Stub() {
            override fun packageDeleted(packageName: String, returnCode: Int) {
                if (returnCode == 1) {
                    Logger.i(packageName + "卸载成功")
                } else {
                    Logger.e(packageName + "卸载失败")
                }
            }
        }
        runCatching {
            mContext.packageManager.deletePackage(packageName, observer, 0)
        }.onFailure { Logger.e("error is:::" + Log.getStackTraceString(it)) }
        return true
    }

    /**
     * 授予应用所有权限
     *
     * @param packageName 应用包名
     *
     * 其中有几个api要适配
     */
    override fun grantAllRuntimePermission(packageName: String) {
        var packageInfo: PackageInfo? = null
        runCatching { mContext.packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS) }.onSuccess { packageInfo = it }
        if (packageInfo == null) return
        //这是什么辣鸡  ？
        val packageInfoNoNull: PackageInfo = packageInfo as PackageInfo
        val shouldGrantPermissionList = packageInfoNoNull.requestedPermissions.filter {
            try {
                val permissionInfo = mContext.packageManager.getPermissionInfo(it, 0)
                mContext.packageManager.checkPermission(permissionInfo.name, packageName) != PackageManager.PERMISSION_GRANTED
            } catch (e: Exception) {
                false
            }
        }
        shouldGrantPermissionList.forEach {
            if (packageInfoNoNull.applicationInfo.targetSdkVersion < Build.VERSION_CODES.M) {
                val opsManager = mContext.getSystemService(AppOpsManager::class.java)
                val appOp = AppOpsManager.permissionToOp(it)
                opsManager?.setUidMode(appOp, packageInfoNoNull.applicationInfo.uid, AppOpsManager.MODE_ALLOWED)
                mContext.packageManager.updatePermissionFlags(it, packageInfoNoNull.packageName,
                        PackageManager.FLAG_PERMISSION_REVOKED_COMPAT, 0, Process.myUserHandle())
            } else {
                mContext.packageManager.grantRuntimePermission(packageName, it, Process.myUserHandle())
            }
        }
    }

}