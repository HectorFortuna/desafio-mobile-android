package br.com.hectorfortuna.ingressoapp.view.home.fragment

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import br.com.hectorfortuna.ingressoapp.core.Status
import br.com.hectorfortuna.ingressoapp.data.model.Item
import br.com.hectorfortuna.ingressoapp.databinding.FragmentHomeBinding
import br.com.hectorfortuna.ingressoapp.view.home.adapter.MovieAdapter
import br.com.hectorfortuna.ingressoapp.view.home.viewmodel.MovieViewModel
import com.facebook.shimmer.ShimmerFrameLayout
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class HomeFragment : Fragment() {
    private val viewModel: MovieViewModel by viewModels()
    private lateinit var shimmerFrameLayout: ShimmerFrameLayout
    private lateinit var binding: FragmentHomeBinding
    private lateinit var movieAdapter: MovieAdapter
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getAllMovies()
        observeVMEvents()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getAllMovies() {
        viewModel.getAllMoviesResponse()
        startShimmer()
    }

    private fun startShimmer() {
        shimmerFrameLayout = binding.shimmerLayout
        shimmerFrameLayout.startShimmer()
    }

    private fun stopShimmer() {
        shimmerFrameLayout.stopShimmer()
        shimmerFrameLayout.hideShimmer()
        shimmerFrameLayout.visibility = View.GONE
    }

    private fun observeVMEvents() {
        viewModel.response.observe(viewLifecycleOwner) {
            if (viewLifecycleOwner.lifecycle.currentState != Lifecycle.State.RESUMED) return@observe
            when (it.status) {
                Status.SUCCESS -> {
                    it.data?.let { response ->
                        Timber.tag("Sucesso").i(response.toString())
                        stopShimmer()
                        setRecyclerView(response.items)

                    }
                }

                Status.ERROR -> {
                    Timber.tag("Erro").e(it.error)
                    stopShimmer()
                }

                Status.LOADING -> {
                    startShimmer()
                }
            }

        }
    }

    private fun setRecyclerView(movieList: List<Item>) {
        movieAdapter = MovieAdapter(movieList){

        }
        binding.rvHomeFragment.apply {
            View.VISIBLE
            setHasFixedSize(true)
            adapter = movieAdapter

        }
    }

}