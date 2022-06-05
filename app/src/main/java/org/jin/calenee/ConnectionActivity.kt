package org.jin.calenee

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import android.widget.Toast
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import org.jin.calenee.MainActivity.Companion.slideLeft
import org.jin.calenee.databinding.ActivityConnectionBinding
import org.jin.calenee.login.LoginActivity

class ConnectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConnectionBinding
    private var inviteCode = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityConnectionBinding.inflate(layoutInflater)

        setContentView(binding.root)

        listener()

    }

    private fun listener() {
        binding.logoutBtn.setOnClickListener {
            val googleSignInOptions: GoogleSignInOptions by lazy {
                GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build()
            }

            val googleSignInClient by lazy {
                GoogleSignIn.getClient(this, googleSignInOptions)
            }

            googleSignInClient.signOut().addOnCompleteListener {
                LoginActivity.viewModel.signOut()
            }

            // set SP
            App.userPrefs.apply {
                setString("current_email", "")
                setString("current_name", "")
                setString("login_status", "false")
            }

            Toast.makeText(this, "로그아웃 되었습니다.", Toast.LENGTH_SHORT).show()

            Intent(this, LoginActivity::class.java).apply {
                startActivity(this)
                slideLeft()
                finish()
            }
        }

        var previousLength = 0
        var backspace: Boolean
        binding.inputCodeEt.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(text: CharSequence?, p1: Int, p2: Int, p3: Int) {
                previousLength = "$text".length
            }

            override fun onTextChanged(p0: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(text: Editable?) {
                backspace = previousLength > "$text".length

                if (!backspace && "$text".length == 4) {
                    text?.append(" ")
                }

                inviteCode = "$text"

                Log.d("text_test/inviteCode", inviteCode)
            }
        })

        binding.inputCodeEt.setOnKeyListener { view, keyCode, keyEvent ->
            if (keyEvent.action == KeyEvent.ACTION_DOWN &&
                keyEvent.keyCode == KeyEvent.KEYCODE_DEL
            ) {
                if (binding.inputCodeEt.text.length == 5) {
                    binding.inputCodeEt.setText(inviteCode.dropLast(1))
                    binding.inputCodeEt.setSelection(binding.inputCodeEt.text.length)

                    Log.d("text_test/del1", "delete1 code length: ${inviteCode.length}")
                }
            }

            return@setOnKeyListener false
        }
    }

    private fun setRandNum() {

    }

    override fun onBackPressed() {}
}