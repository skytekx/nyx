package de.ellpeck.nyx.entities;

import de.ellpeck.nyx.capabilities.NyxWorld;
import de.ellpeck.nyx.lunarevents.BloodMoon;
import de.ellpeck.nyx.lunarevents.FullMoon;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.target.NonTameRandomTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.player.Player;

/**
 * During a full or blood moon, untamed wolves hunt players, animals and
 * skeletons (but not other wolves). Ported from {@code EntityAITargetNonTamed}.
 */
public class WolfAiSpecialMoon extends NonTameRandomTargetGoal<LivingEntity> {

    public WolfAiSpecialMoon(TamableAnimal entityIn) {
        super(entityIn, LivingEntity.class, false, e -> {
            if (e instanceof Wolf)
                return false;
            return e instanceof Player || e instanceof Animal || e instanceof Skeleton;
        });
    }

    @Override
    public boolean canUse() {
        return super.canUse() && this.shouldHappen();
    }

    @Override
    public boolean canContinueToUse() {
        return super.canContinueToUse() && this.shouldHappen();
    }

    private boolean shouldHappen() {
        NyxWorld nyx = NyxWorld.get(this.mob.level());
        if (nyx == null)
            return false;
        return nyx.currentEvent instanceof FullMoon || nyx.currentEvent instanceof BloodMoon;
    }
}
