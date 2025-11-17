package com.music.innertube.models

import kotlinx.serialization.Serializable

//@Serializable
//data class SubscriptionButton(
//    val subscribeButtonRenderer: SubscribeButtonRenderer,
//) {
//    @Serializable
//    data class SubscribeButtonRenderer(
//        val subscribed: Boolean,
//        val channelId: String,
//    )
//}

//addded sub number showing
@Serializable
data class SubscriptionButton(
    val subscribeButtonRenderer: SubscribeButtonRenderer,
) {
    @Serializable
    data class SubscribeButtonRenderer(
        val subscribed: Boolean,
        val channelId: String,
        val subscriberCountText: Runs? = null,  // ADD THIS LINE
    )
}