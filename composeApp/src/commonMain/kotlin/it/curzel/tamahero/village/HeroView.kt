package it.curzel.tamahero.village

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.curzel.tamahero.models.Hero
import it.curzel.tamahero.models.HeroConfig
import it.curzel.tamahero.models.Resources
import it.curzel.tamahero.network.GameSocketClient
import it.curzel.tamahero.ui.theme.*

@Composable
fun HeroView(
    hero: Hero,
    resources: Resources,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val nextLevelXp = HeroConfig.xpForLevel(hero.level + 1)
    val xpProgress = if (nextLevelXp > 0) (hero.xp.toFloat() / nextLevelXp).coerceIn(0f, 1f) else 1f
    val canFeed = resources.hasEnough(Resources(gold = HeroConfig.FEED_COST_GOLD))
    val canTrain = resources.hasEnough(Resources(mana = HeroConfig.TRAIN_COST_MANA))

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(TamaColors.Background.copy(alpha = 0.5f))
            .clickable(onClick = onDismiss),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = TamaRadius.Large, topEnd = TamaRadius.Large))
                .background(TamaColors.Surface)
                .clickable(enabled = false, onClick = {})
                .padding(TamaSpacing.Medium),
        ) {
            Text("Hero (Level ${hero.level})", color = TamaColors.Text, fontSize = 18.sp)
            Spacer(Modifier.height(TamaSpacing.Small))

            Text("XP: ${hero.xp}/$nextLevelXp", color = TamaColors.TextMuted, fontSize = 14.sp)
            LinearProgressIndicator(
                progress = { xpProgress },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = TamaColors.Accent,
                trackColor = TamaColors.SurfaceElevated,
            )
            Spacer(Modifier.height(TamaSpacing.XXSmall))

            val hungerColor = when {
                hero.hunger > 50 -> TamaColors.Success
                hero.hunger > 25 -> TamaColors.Warning
                else -> TamaColors.Danger
            }
            Text("Hunger: ${hero.hunger}/100", color = hungerColor, fontSize = 14.sp)
            LinearProgressIndicator(
                progress = { hero.hunger / 100f },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = hungerColor,
                trackColor = TamaColors.SurfaceElevated,
            )
            Spacer(Modifier.height(TamaSpacing.XXSmall))

            val happyColor = when {
                hero.happiness > 50 -> TamaColors.Success
                hero.happiness > 25 -> TamaColors.Warning
                else -> TamaColors.Danger
            }
            Text("Happiness: ${hero.happiness}/100", color = happyColor, fontSize = 14.sp)
            LinearProgressIndicator(
                progress = { hero.happiness / 100f },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = happyColor,
                trackColor = TamaColors.SurfaceElevated,
            )

            Spacer(Modifier.height(TamaSpacing.XSmall))
            Text("Bonus damage: +${HeroConfig.bonusDamagePercent(hero.level)}%", color = TamaColors.TextMuted, fontSize = 14.sp)

            Spacer(Modifier.height(TamaSpacing.Small))
            Row(
                horizontalArrangement = Arrangement.spacedBy(TamaSpacing.XSmall),
            ) {
                TamaButton(
                    text = "Feed (${HeroConfig.FEED_COST_GOLD}g)",
                    color = TamaColors.Success,
                    enabled = canFeed,
                    onClick = { GameSocketClient.feedHero() },
                    modifier = Modifier.weight(1f),
                )
                TamaButton(
                    text = "Train (${HeroConfig.TRAIN_COST_MANA}m)",
                    color = TamaColors.Accent,
                    enabled = canTrain,
                    onClick = { GameSocketClient.trainHero() },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
