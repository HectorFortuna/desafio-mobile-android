package br.com.hectorfortuna.ingressoapp.view.home.viewmodel

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.hectorfortuna.ingressoapp.core.State
import br.com.hectorfortuna.ingressoapp.data.model.AllMoviesResponse
import br.com.hectorfortuna.ingressoapp.data.model.Item
import br.com.hectorfortuna.ingressoapp.data.repository.AllMoviesRepository
import br.com.hectorfortuna.ingressoapp.di.coroutines.qualifiers.Io
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject

@HiltViewModel
class MovieViewModel @Inject constructor(
    private val moviesRepository: AllMoviesRepository,
    @Io private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {
    private val _response = MutableLiveData<State<AllMoviesResponse>>()
    val response: LiveData<State<AllMoviesResponse>> = _response

    @RequiresApi(Build.VERSION_CODES.O)
    fun getAllMoviesResponse() {
        viewModelScope.launch {
            try {
                _response.value = State.loading(true)
                val response = withContext(ioDispatcher) {
                    moviesRepository.getAllMovies()
                }
                response.items.let {
                    val sortedItems = sortItemsByPremiereDate(it)
                    val sortedResponse = response.copy(items = sortedItems)  // Atualiza a lista ordenada

                    // Passa a resposta com os itens ordenados para o LiveData
                    _response.value = State.success(sortedResponse)
                }
            } catch (throwable: Throwable) {
                _response.value = State.loading(false)
                _response.value = State.error(throwable)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun sortItemsByPremiereDate(items: List<Item>): List<Item> {
        val now = LocalDateTime.now()  // Obter a data e hora atual

        return items.sortedWith { item1, item2 ->
            // Tenta fazer o parsing de cada premiereDate de forma segura
            val premiereDate1 = item1.premiereDate?.localDate?.let { parsePremiereDate(it) }
            val premiereDate2 = item2.premiereDate?.localDate?.let { parsePremiereDate(it) }

            // Caso ambos tenham premiereDate, ordena pela data
            if (premiereDate1 != null && premiereDate2 != null) {
                val diff1 = ChronoUnit.MINUTES.between(now, premiereDate1)
                val diff2 = ChronoUnit.MINUTES.between(now, premiereDate2)
                return@sortedWith diff1.compareTo(diff2)  // Compara o tempo restante até o lançamento
            }

            // Se item1 tem premiereDate e item2 não, coloca item1 primeiro
            if (premiereDate1 != null) {
                return@sortedWith -1  // item1 vem antes
            }

            // Se item2 tem premiereDate e item1 não, coloca item2 primeiro
            if (premiereDate2 != null) {
                return@sortedWith 1  // item2 vem antes
            }

            // Se ambos não têm premiereDate, mantém a ordem original
            return@sortedWith 0
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun parsePremiereDate(localDate: String): LocalDateTime {
        val formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
        return LocalDateTime.parse(localDate, formatter)
    }

}