package org.jin.calenee.database.model

import com.google.firebase.firestore.DocumentId

data class CoupleConnection(
    var ownerInviteCode: String = "",
    var ownerEmail: String = "",
    var partnerEmail: String = "",
    var codeExpirationFlag: Boolean = false,
    var connectionFlag: Boolean = false,
    @DocumentId
    val id: String? = null
)