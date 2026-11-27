package com.zirakusa.telefono.mcp

import android.app.Activity
import android.app.AlertDialog
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.Toast
import android.widget.Switch
import android.widget.TextView
import com.zirakusa.telefono.R

internal fun showMcpOptions(activity: Activity) {
    val gap = (16 * activity.resources.displayMetrics.density).toInt()
    val controls = LinearLayout(activity).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(gap, gap / 2, gap, 0)
    }
    fun option(label: Int, value: Boolean, change: (Boolean) -> Unit) {
        controls.addView(Switch(activity).apply {
            setText(label)
            gravity = Gravity.CENTER_VERTICAL
            minHeight = gap * 3
            isChecked = value
            setOnCheckedChangeListener { _, on -> change(on) }
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
    }
    option(R.string.mcp_option_enable, McpMode.enabled, McpMode::enable)
    option(R.string.mcp_option_local, McpMode.localMcp, McpMode::allowLocal)
    option(R.string.mcp_option_redirect, McpMode.remote, McpMode::turnRemote)
    McpProbeReceipt.id?.let { id ->
        controls.addView(TextView(activity).apply { text = activity.getString(R.string.mcp_option_probe, id) })
    }
    val dialog = AlertDialog.Builder(activity).setTitle(R.string.mcp_option_title)
        .setView(controls).setPositiveButton(android.R.string.ok, null)
    dialog.setNeutralButton(R.string.mcp_option_open_hub) { _, _ ->
        val launch = activity.packageManager.getLaunchIntentForPackage("com.zirakusa.gatewayhub")
        if (launch != null) activity.startActivity(launch)
        else Toast.makeText(activity, R.string.mcp_no_arka, Toast.LENGTH_LONG).show()
    }
    dialog.show()
}
