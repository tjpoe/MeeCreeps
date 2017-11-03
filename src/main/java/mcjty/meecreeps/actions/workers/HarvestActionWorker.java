package mcjty.meecreeps.actions.workers;

import mcjty.meecreeps.api.IActionOptions;
import mcjty.meecreeps.entities.EntityMeeCreeps;
import mcjty.meecreeps.varia.GeneralTools;
import mcjty.meecreeps.varia.SoundTools;
import net.minecraft.block.Block;
import net.minecraft.block.BlockCrops;
import net.minecraft.block.BlockNetherWart;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.IPlantable;

import java.util.ArrayList;
import java.util.List;

public class HarvestActionWorker extends AbstractActionWorker {

    private AxisAlignedBB actionBox = null;

    public HarvestActionWorker(EntityMeeCreeps entity, IActionOptions options) {
        super(entity, options);
    }

    @Override
    protected AxisAlignedBB getActionBox() {
        if (actionBox == null) {
            // @todo config
            actionBox = new AxisAlignedBB(options.getTargetPos().add(-10, -5, -10), options.getTargetPos().add(10, 5, 10));
        }
        return actionBox;
    }

    protected void harvest(BlockPos pos) {
        World world = entity.getEntityWorld();
        IBlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        List<ItemStack> drops = block.getDrops(world, pos, state, 0);
        net.minecraftforge.event.ForgeEventFactory.fireBlockHarvesting(drops, world, pos, state, 0, 1.0f, false, GeneralTools.getHarvester());
        SoundTools.playSound(world, block.getSoundType().getBreakSound(), pos.getX(), pos.getY(), pos.getZ(), 1.0f, 1.0f);
        entity.getEntityWorld().setBlockToAir(pos);
        for (ItemStack stack : drops) {
            ItemStack remaining = entity.addStack(stack);
            if (!remaining.isEmpty()) {
                itemsToPickup.add(entity.entityDropItem(remaining, 0.0f));
                needsToPutAway = true;
            }
        }
    }


    @Override
    protected void performTick(boolean timeToWrapUp) {
        if (timeToWrapUp) {
            done();
        } else {
            tryFindingCropsToHarvest();
        }
    }

    protected void tryFindingCropsToHarvest() {
        AxisAlignedBB box = getActionBox();
        World world = entity.getEntityWorld();
        List<BlockPos> positions = new ArrayList<>();
        GeneralTools.traverseBox(world, box,
                (pos, state) -> state.getBlock() == Blocks.FARMLAND && allowedToHarvest(state, world, pos, GeneralTools.getHarvester()),
                (pos, state) -> {
                    IBlockState cropState = world.getBlockState(pos.up());
                    Block cropBlock = cropState.getBlock();
                    boolean hasCrops = cropBlock instanceof IPlantable && state.getBlock().canSustainPlant(world.getBlockState(pos), world, pos, EnumFacing.UP, (IPlantable) cropBlock);
                    if (hasCrops) {
                        if (cropBlock instanceof BlockCrops) {
                            BlockCrops crops = (BlockCrops) cropBlock;
                            int age = crops.getAge(cropState);
                            int maxAge = crops.getMaxAge();
                            if (age >= maxAge) {
                                positions.add(pos.up());
                            }
                        } else if (cropBlock instanceof BlockNetherWart) {
                            int age = cropState.getValue(BlockNetherWart.AGE);
                            int maxAge = 3;
                            if (age >= maxAge) {
                                positions.add(pos.up());
                            }
                        }
                    }
                });
        if (!positions.isEmpty()) {
            BlockPos cropPos = positions.get(0);
            navigateTo(cropPos, this::harvest);
        } else if (!entity.getInventory().isEmpty()) {
            needsToPutAway = true;
        }
    }

}
