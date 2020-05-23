package com.renai.android.tashcabs.screens.account

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.parse.ParseUser
import com.renai.android.tashcabs.R
import com.renai.android.tashcabs.databinding.FragmentAccountBinding


private const val TAG = "CommonLogs"

class AccountFragment : Fragment() {
    private lateinit var binding: FragmentAccountBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentAccountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        binding.logoutBtn.setOnClickListener {
            ParseUser.logOutInBackground {
                if (it == null) {
                    findNavController().navigate(R.id.action_account_dest_to_login_dest)
                } else throw it
            }
        }
    }
}
