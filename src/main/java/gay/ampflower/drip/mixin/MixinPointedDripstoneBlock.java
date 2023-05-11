package gay.ampflower.drip.mixin;

import gay.ampflower.drip.Config;
import net.minecraft.block.BlockState;
import net.minecraft.block.PointedDripstoneBlock;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Mixin into {@link PointedDripstoneBlock} to add alternative returns for
 * various blocks.
 *
 * @author Ampflower
 * @since 0.0.0
 **/
@Mixin(PointedDripstoneBlock.class)
public class MixinPointedDripstoneBlock {

	/**
	 * Redirects blocks to return a different fluid if configured in
	 * {@link Config#entries}.
	 *
	 * @param pos
	 *            The position of the block.
	 * @param fluid
	 *            The old fluid that may be replaced.
	 * @param state
	 *            The current block state.
	 * @return New fluid if replaced, else old fluid.
	 */
	@ModifyArg(method = "method_33279", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/PointedDripstoneBlock$DrippingFluid;<init>(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/fluid/Fluid;Lnet/minecraft/block/BlockState;)V"))
	private static Fluid drip$getFluid(BlockPos pos, Fluid fluid, BlockState state) {
		var entry = Config.INSTANCE.entries.get(state.getBlock());
		if (entry != null && (entry.replace() || (fluid == null || fluid == Fluids.EMPTY))) {
			return entry.fluid();
		}
		return fluid;
	}
}
