package com.example.wordsapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.wordsapp.databinding.FragmentWordListBinding

private var _binding: FragmentWordListBinding? = null
private val binding get() = _binding!!


class WordListFragment : Fragment() {
    companion object {
        val LETTER = "letter"
        val SEARCH_PREFIX = "https://www.google.com/search?q="
    }

    private lateinit var letterID:String

    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)

        arguments?.let{
            letterID = it.getString(LETTER).toString()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentWordListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val recyclerView = binding.recyclerView
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = WordAdapter(letterID, requireContext())

        recyclerView.addItemDecoration(
            DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


}