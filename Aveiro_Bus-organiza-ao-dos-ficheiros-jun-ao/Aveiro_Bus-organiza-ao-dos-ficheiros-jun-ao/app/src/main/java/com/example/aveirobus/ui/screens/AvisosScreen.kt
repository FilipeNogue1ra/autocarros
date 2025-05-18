package com.example.aveirobus.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog // Import para Dialog (Pop-up)
import androidx.compose.ui.window.DialogProperties // Import para propriedades do Dialog

// Data class to represent an Aviso (Notice)
data class Aviso(val id: Int, val title: String, val content: String, val detailedInfo: String)

@Composable
fun Avisos(paddingValues: PaddingValues) {
    val sampleAvisos = remember {
        listOf(
            Aviso(
                1,
                "Atualização de Horários do Terminal",
                "Alterações nos horários da bilheteira e do terminal a partir de 1 de maio.",
                "Informamos que, a partir do dia 1 de maio, os horários do Terminal Rodoviário de Aveiro e da nossa bilheteira irão sofrer alterações.\n\n" +
                        "Horário do Terminal Rodoviário:\n" +
                        "Seg. a Qui. (exceto feriados): 07:00–19:00\n" +
                        "Sexta (ou quinta se sexta feriado): 07:00–20:00\n" +
                        "Sábado: 08:00–18:30\n" +
                        "Domingos (ou segunda se feriado): 08:00–20:00\n" +
                        "Feriados (exceto quando segunda for feriado): 08:00–18:30\n\n" +
                        "Horário da Bilheteira:\n" +
                        "Seg. a Qui. (exceto feriados): 07:00–19:00\n" +
                        "Sexta (ou quinta se sexta feriado): 07:00–20:00\n" +
                        "Sábado: 08:00–11:00 / 13:00–18:30\n" +
                        "Domingos (ou segunda se feriado): 08:00–11:00 / 13:00–17:15 / 17:45–20:00\n" +
                        "Feriados (exceto quando segunda for feriado): 08:00–11:00 / 13:00–18:30"
            ),
            Aviso(
                2,
                "L3: Condicionamento Temporário",
                "Percurso da Linha 3 condicionado a partir de 26 de março.",
                "A partir de 26 de março de 2025 e durante cerca de 7 semanas, o percurso da L3 estará condicionado na R. D. Sancho I devido a obras.\n\n" +
                        "Sentido Aveiro > Zona Industrial:\n" +
                        "Paragens desativadas: General Costa Cascais B, Tanques de Esgueira B, Rua S. Bartolomeu, Qta. Cardadeiras.\n" +
                        "Alternativas: Igreja de Esgueira B, paragem temporária junto ao Pingo Doce.\n\n" +
                        "Sentido Zona Industrial > Aveiro:\n" +
                        "Paragens desativadas: Casa das Framboesas, Tanques de Esgueira A, Gen. Costa Cascais A.\n" +
                        "Alternativas: Paragem junto ao Pingo Doce, Igreja de Esgueira A.\n\n" +
                        "Paragem Travessa do Eucalipto também será desativada. Use a paragem Eucalipto B."
            ),
            Aviso(
                3,
                "L4: Alteração Temporária de Percurso",
                "Percurso da Linha 4 condicionado na zona das Alagoas.",
                "Desde 14 de março e durante cerca de 5 semanas, o percurso da L4 estará condicionado na zona das Alagoas devido a obras.\n\n" +
                        "Sentido Aveiro > Eixo/Carregal:\n" +
                        "Paragens desativadas: Tanques de Esgueira B, R. 31 Janeiro/Griné B.\n" +
                        "Alternativas: R. Viso 1B, paragem temporária na Rua da Prata com Rua 31 de Janeiro.\n\n" +
                        "Sentido Carregal/Eixo > Aveiro:\n" +
                        "Paragens desativadas: Tanques de Esgueira A, R. 31 Janeiro/Griné A.\n" +
                        "Alternativas: R. Viso 1A, paragem temporária na Rua da Prata com Rua 31 de Janeiro."
            ),
            Aviso(
                4,
                "L07: Condicionamento de Percurso",
                "Linha 07 com percurso alterado desde 28 de novembro.",
                "A partir de 28 de novembro e por tempo indeterminado, o percurso da L07 estará condicionado devido a obras.\n" +
                        "Paragens desativadas: Escola Areias de Vilar A/B.\n" +
                        "Alternativas: Santa Eufémia A/B e Areais A/B."
            )
        )
    }


    // Estado para rastrear qual aviso está sendo mostrado no pop-up (null = nenhum)
    var avisoToShowInPopup by remember { mutableStateOf<Aviso?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp)
    ) {
        Text(
            text = "Últimos Avisos",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(sampleAvisos, key = { it.id }) { aviso ->
                AvisoCardForPopup( // Usaremos uma versão diferente do Card
                    aviso = aviso,
                    onCardClick = { clickedAviso ->
                        avisoToShowInPopup = clickedAviso // Define o aviso a ser mostrado no pop-up
                    }
                )
            }
        }
    }

    // Mostra o pop-up se avisoToShowInPopup não for null
    avisoToShowInPopup?.let { aviso ->
        AlertDialog(
            onDismissRequest = {
                // Quando o usuário clica fora do diálogo ou pressiona back, fecha o pop-up
                avisoToShowInPopup = null
            },
            title = {
                Text(text = aviso.title)
            },
            text = {
                // Conteúdo detalhado do aviso no pop-up
                Text(text = aviso.detailedInfo)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        avisoToShowInPopup = null // Fecha o pop-up ao clicar no botão
                    }
                ) {
                    Text("Fechar")
                }
            }
        )
    }
}

// Função composable para exibir um cartão de aviso que abre um pop-up
@Composable
fun AvisoCardForPopup(
    aviso: Aviso,
    onCardClick: (Aviso) -> Unit // Função a ser chamada ao clicar no cartão
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick(aviso) } // Torna o cartão clicável
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = aviso.title,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            // Mostra o conteúdo inicial no cartão
            Text(
                text = aviso.content,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
