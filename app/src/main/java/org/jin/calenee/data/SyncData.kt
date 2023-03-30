package org.jin.calenee.data

import org.jin.calenee.App
import org.jin.calenee.data.firestore.CoupleInfoSync
import org.jin.calenee.data.firestore.UserSync

class SyncData {
    fun syncUserData(user: UserSync, email: String) {
        App.userPrefs.updateUserData(user, email)
    }

    fun syncCoupleInfoData(coupleInfo: CoupleInfoSync) {
        App.userPrefs.updateCoupleInfoData(coupleInfo)
    }
}