package org.jin.calenee.data.firestore

data class UserSync(
    val fcmToken: String = "", // my_fcm_token
    val nickname: String = "", // current_nickname
    val birthday: String = "", // current_birthday
    val gender: String = "", // current_gender
    val coupleChatID: String = "", //couple_chat_id
    val partnerEmail: String = "", // current_partner_email
    val partnerFCMToken: String = "", // partner_fcm_token
    val coupleConnectionFlag: Boolean = false, // ${email}_couple_connection_flag
    val profileImageFlag: Boolean = false, // ${email}_profile_image_flag
    val profileInputFlag: Boolean = false, // ${email}_couple_input_flag
)
