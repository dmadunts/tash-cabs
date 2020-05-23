package com.renai.android.tashcabs.screens.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.parse.ParseUser
import com.renai.android.tashcabs.R
import com.renai.android.tashcabs.databinding.FragmentLoginBinding
import com.renai.android.tashcabs.parse.handleParseError
import com.renai.android.tashcabs.utils.coordinateBtnAndInputs


class LoginFragment : Fragment() {
    private lateinit var binding: FragmentLoginBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentLoginBinding.inflate(inflater)

        return binding.root
    }


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

//        activity!!.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN


        coordinateBtnAndInputs(binding.loginButton, binding.usernameInput, binding.passwordInput)

        binding.loginButton.setOnClickListener {
            val username = binding.usernameInput.text.toString()
            val password = binding.passwordInput.text.toString()
            ParseUser.logInInBackground(username, password) { _, e ->
                if (e == null) {
                    findNavController().navigate(R.id.action_login_dest_to_rider_dest)
                } else
                    handleParseError(e)
            }
        }

        binding.signUpText.setOnClickListener {
            findNavController().navigate(R.id.signup_dest)
        }

//        binding.loginButton.setOnEditorActionListener(OnEditorActionListener { v, actionId, event ->
//            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
//                // hide virtual keyboard
//                val imm: InputMethodManager? = context!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
//                imm?.hideSoftInputFromWindow(
//                    binding.passwordInput.windowToken,
//                    InputMethodManager.RESULT_UNCHANGED_SHOWN
//                )
//                return@OnEditorActionListener true
//            }
//            false
//        })
    }


    override fun onDestroy() {
        //we always want to go to RiderFragment and clear back stack
        findNavController().popBackStack(R.id.rider_dest, true)
        super.onDestroy()
    }
}
