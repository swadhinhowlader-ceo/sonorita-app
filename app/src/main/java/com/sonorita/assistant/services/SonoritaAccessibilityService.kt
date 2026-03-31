package com.sonorita.assistant.services

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class SonoritaAccessibilityService : AccessibilityService() {

    companion object {
        var instance: SonoritaAccessibilityService? = null
            private set
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this

        serviceInfo = serviceInfo?.apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                         AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                         AccessibilityEvent.TYPE_VIEW_CLICKED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                    AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            notificationTimeout = 100
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                val packageName = event.packageName?.toString() ?: return
                // Broadcast foreground app change
                val intent = Intent("com.sonorita.FOREGROUND_APP").apply {
                    putExtra("package", packageName)
                    setPackage(packageName)
                }
                sendBroadcast(intent)
            }
            AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                // Track user interactions for learning mode
            }
        }
    }

    override fun onInterrupt() {
        // Handle interruption
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    // Utility methods for autopilot

    fun findNodeByText(root: AccessibilityNodeInfo?, text: String): AccessibilityNodeInfo? {
        if (root == null) return null

        if (root.text?.toString()?.contains(text, ignoreCase = true) == true) {
            return root
        }

        if (root.contentDescription?.toString()?.contains(text, ignoreCase = true) == true) {
            return root
        }

        for (i in 0 until root.childCount) {
            val child = root.getChild(i) ?: continue
            val found = findNodeByText(child, text)
            if (found != null) return found
        }

        return null
    }

    fun clickNode(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false

        return if (node.isClickable) {
            node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        } else {
            clickNode(node.parent)
        }
    }

    fun scrollToFind(root: AccessibilityNodeInfo?, text: String): Boolean {
        root ?: return false

        // Try to find and click a scrollable parent
        val scrollable = findScrollableParent(root)
        scrollable?.let {
            return it.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
        }

        return false
    }

    private fun findScrollableParent(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.isScrollable) return node
        return findScrollableParent(node.parent)
    }

    fun typeText(node: AccessibilityNodeInfo?, text: String): Boolean {
        if (node == null) return false

        val arguments = android.os.Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
    }

    fun getRootNode(): AccessibilityNodeInfo? {
        return rootInActiveWindow
    }

    fun getAppPackageName(): String? {
        return rootInActiveWindow?.packageName?.toString()
    }
}
