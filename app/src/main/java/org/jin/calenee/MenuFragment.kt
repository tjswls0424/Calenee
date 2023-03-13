package org.jin.calenee

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import org.jin.calenee.MainActivity.Companion.slideLeft
import org.jin.calenee.databinding.FragmentMenuBinding
import org.jin.calenee.login.LoginActivity

class MenuFragment : Fragment(R.layout.fragment_menu) {
    private lateinit var mContext: Context
    private lateinit var mActivity: Activity

    private val binding: FragmentMenuBinding by lazy {
        FragmentMenuBinding.inflate(layoutInflater)
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
                logout()
            }
            setNegativeButton("취소") { dialog, _ ->
                dialog.dismiss()
            }
        }.show()
    }

    private fun logout() {
        val googleSignInOptions: GoogleSignInOptions by lazy {
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
        }

        val googleSignInClient by lazy {
            GoogleSignIn.getClient(mActivity, googleSignInOptions)
        }

        googleSignInClient.signOut().addOnCompleteListener {
            LoginActivity.viewModel.signOut()
        }

        // set SP
        App.userPrefs.apply {
            setString("login_status", "false")
            setString("current_email", "")
            setString("current_nickname", "")
            setString("current_birthday", "")
            setString("current_partner_email", "")
            setString("current_partner_nickname", "")
            setString("current_partner_birthday", "")
        }

        Toast.makeText(mActivity, "로그아웃 되었습니다.", Toast.LENGTH_SHORT).show()

        Intent(mActivity, LoginActivity::class.java).apply {
            startActivity(this)
            mActivity.slideLeft()
            finishFragment()
        }
    }

    private fun finishFragment() {
        activity?.supportFragmentManager
            ?.beginTransaction()
            ?.remove(this@MenuFragment)
            ?.commit()
    }
}