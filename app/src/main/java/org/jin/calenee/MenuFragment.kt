package org.jin.calenee

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.*
import org.jin.calenee.MainActivity.Companion.slideLeft
import org.jin.calenee.databinding.FragmentMenuBinding
import org.jin.calenee.login.LoginActivity

class MenuFragment : Fragment(R.layout.fragment_menu) {
    private val mainScope = CoroutineScope(Dispatchers.Main)

    private lateinit var mContext: Context
    private lateinit var mActivity: Activity

    private val binding: FragmentMenuBinding by lazy {
        FragmentMenuBinding.inflate(layoutInflater)
    }
    private val firebaseAuth: FirebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mActivity = context as MainActivity
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        setMyEmail()
        listener()

        return binding.root
    }

    private fun listener() {
        binding.logoutBtn.setOnClickListener {
            showDialog()
        }
    }

    private fun setMyEmail() {
        binding.myAccountTv.text = App.userPrefs.getString("current_email")
    }

    private fun showDialog() {
        AlertDialog.Builder(mActivity).apply {
            setMessage("로그아웃 하시겠습니까?")
            setPositiveButton("확인") { _, _ ->
                mainScope.launch {
                    val deferred = async {
                        logout()
                        return@async true
                    }

                    if (deferred.await()) {
//                        moveToLoginActivity()
                    }
                }
            }
            setNegativeButton("취소") { dialog, _ ->
                dialog.dismiss()
            }
        }.show()
    }

    private suspend fun logout() {
        withContext(Dispatchers.Main) {
            val email = firebaseAuth.currentUser?.email.toString()
            launch(Dispatchers.IO) {
                if (email.endsWith("@gmail.com")) {
                    // google login
                    val googleSignInOptions: GoogleSignInOptions by lazy {
                        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestIdToken(getString(R.string.default_web_client_id))
                            .requestEmail()
                            .build()
                    }

                    val googleSignInClient by lazy {
                        GoogleSignIn.getClient(mActivity, googleSignInOptions)
                    }

                    try {
                        googleSignInClient.signOut()
                            .addOnSuccessListener {
                                LoginActivity.viewModel.signOut()

                                mainScope.launch {
                                    val res = withContext(Dispatchers.Main) {
                                        App.userPrefs.clearUserData(firebaseAuth.currentUser?.email.toString())
                                        true
                                    }

                                    if (res) {
                                        moveToLoginActivity()
                                    }
                                }
                            }
                            .addOnFailureListener {
                                Snackbar.make(
                                    binding.root,
                                    "로그아웃에 실패했습니다. 잠시후 재시도해주세요.",
                                    Snackbar.LENGTH_SHORT
                                ).show()
                            }

                    } catch (e: Exception) {
                        Log.d("logout_test", e.stackTraceToString())
                    }
                } else {
                    // calenee login
                    LoginActivity.viewModel.signOut()
                    firebaseAuth.signOut()

                    mainScope.launch {
                        val res = withContext(Dispatchers.Main) {
                            App.userPrefs.clearUserData(firebaseAuth.currentUser?.email.toString())
                            true
                        }

                        if (res) {
                            moveToLoginActivity()
                        }
                    }
                }
            }
        }
    }

    private fun moveToLoginActivity() {
        mainScope.launch {
            Toast.makeText(mActivity, "로그아웃 되었습니다.", Toast.LENGTH_SHORT).show()

            Intent(mActivity, LoginActivity::class.java).also {
                startActivity(it)
                mActivity.slideLeft()

//            childFragmentManager.apply {
//                beginTransaction().remove(this@MenuFragment).commit()
//                popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
//            }
                mActivity.finish()
            }
        }
    }

//    private fun finishFragment() {
//        activity?.supportFragmentManager
//            ?.beginTransaction()
//            ?.remove(this@MenuFragment)
//            ?.commit()
//    }
}