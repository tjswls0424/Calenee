package org.jin.calenee.login

import android.net.Uri

sealed class LoginState {
    object UnInitialized : LoginState()
    object Loading : LoginState()

    data class Login(
        val idToken: String
    ) : LoginState()

    sealed class Success : LoginState() {
        data class Registered(
            // google auth 등록된 상태
            val userName: String,
            val profileImgUri: Uri
        ) : Success()

        object NotRegistered : Success() // google auth 미등록 상태
    }

    object Error : LoginState()
}