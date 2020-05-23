package com.renai.android.tashcabs.screens.signup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.parse.ParseUser
import com.renai.android.tashcabs.R
import com.renai.android.tashcabs.databinding.FragmentSignupBinding
import com.renai.android.tashcabs.parse.handleParseError
import com.renai.android.tashcabs.utils.coordinateBtnAndInputs

class SignUpFragment : Fragment() {

    private lateinit var binding: FragmentSignupBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSignupBinding.inflate(inflater)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        coordinateBtnAndInputs(binding.signupBtn, binding.usernameEdit, binding.passwordEdit, binding.passwordRepeatEdit)

        binding.signupBtn.setOnClickListener {
            val pass1 = binding.passwordEdit.text.toString()
            val pass2 = binding.passwordRepeatEdit.text.toString()

            if (checkPasswordEquality(pass1, pass2)) {
                val user = ParseUser()
                user.username = binding.usernameEdit.text.toString()
                user.setPassword(binding.passwordEdit.text.toString())
                user.signUpInBackground { e ->
                    if (e == null) {
                        findNavController().navigate(R.id.action_signup_dest_to_rider_dest)
                    } else handleParseError(e)
                }
            } else {
                binding.passwordInputLayout.error = getString(R.string.passwords_not_same)
                binding.passwordRepeatInputLayout.error = getString(R.string.passwords_not_same)
            }
        }
    }

    private fun checkPasswordEquality(pass1: String, pass2: String): Boolean = pass1 == pass2
}
