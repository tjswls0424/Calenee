package org.jin.calenee.database.firestore

data class CoupleConnection(
    var ownerInviteCode: String = "",
    var ownerEmail: String = "",
    var partnerEmail: String = "",
    var codeExpirationFlag: Boolean = false,
    var connectionFlag: Boolean = false
)