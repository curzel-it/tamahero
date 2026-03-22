package it.curzel.tamahero.village

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.curzel.tamahero.models.Resources

@Composable
fun GameHudView(
    resources: Resources,
    isPlacing: Boolean,
    onBuildClick: () -> Unit,
    onCancelClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.6f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Gold: ${resources.gold}", color = Color(0xFFFFD700), fontSize = 14.sp)
        Text("Wood: ${resources.wood}", color = Color(0xFF8B4513), fontSize = 14.sp)
        Text("Metal: ${resources.metal}", color = Color(0xFFB0B0B0), fontSize = 14.sp)
        Text("Mana: ${resources.mana}", color = Color(0xFF4169E1), fontSize = 14.sp)
        Spacer(Modifier.width(8.dp))
        if (isPlacing) {
            Button(
                onClick = onCancelClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336)),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            ) {
                Text("Cancel", fontSize = 13.sp)
            }
        } else {
            Button(
                onClick = onBuildClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            ) {
                Text("Build", fontSize = 13.sp)
            }
        }
    }
}
