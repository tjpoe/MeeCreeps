package mcjty.meecreeps.actions.workers;

import mcjty.meecreeps.actions.ActionOptions;
import mcjty.meecreeps.entities.EntityMeeCreeps;
import mcjty.meecreeps.varia.GeneralTools;
import mcjty.meecreeps.varia.SoundTools;
import net.minecraft.entity.EntityLiving;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldEntitySpawner;

public class LightupActionWorker extends AbstractActionWorker {

    private AxisAlignedBB actionBox = null;

    @Override
    protected AxisAlignedBB getActionBox() {
        if (actionBox == null) {
            // @todo config
            actionBox = new AxisAlignedBB(options.getPos().add(-10, -5, -10), options.getPos().add(10, 5, 10));
        }
        return actionBox;
    }


    public LightupActionWorker(EntityMeeCreeps entity, ActionOptions options) {
        super(entity, options);
    }

    private BlockPos findDarkSpot() {
        World world = entity.getEntityWorld();
        AxisAlignedBB box = getActionBox();
        return GeneralTools.traverseBoxFirst(box, p -> {
            if (WorldEntitySpawner.canCreatureTypeSpawnAtLocation(EntityLiving.SpawnPlacementType.ON_GROUND, world, p)) {
                int light = world.getLightFromNeighbors(p);
                if (light < 7) {
                    return p;
                }
            }
            return null;
        });
    }

    private void placeTorch(BlockPos pos) {
        World world = entity.getEntityWorld();
        int light = world.getLightFromNeighbors(pos);
        if (light < 7) {
            ItemStack torch = entity.consumeItem(this::isTorch, 1);
            if (!torch.isEmpty()) {
                entity.getEntityWorld().setBlockState(pos, Blocks.TORCH.getDefaultState(), 3);
                SoundTools.playSound(world, Blocks.TORCH.getSoundType().getPlaceSound(), pos.getX(), pos.getY(), pos.getZ(), 1.0f, 1.0f);
            }
        }
    }

    private boolean isTorch(ItemStack stack) {
        return stack.getItem() == Item.getItemFromBlock(Blocks.TORCH);
    }

    @Override
    protected void performTick(boolean timeToWrapUp) {
        if (timeToWrapUp) {
            done();
        } else if (!entity.hasItem(this::isTorch)) {
            findItemOnGroundOrInChest(this::isTorch, "I cannot find any torches");
        } else {
            BlockPos darkSpot = findDarkSpot();
            if (darkSpot != null) {
                navigateTo(darkSpot, this::placeTorch);
            } else {
                taskIsDone();
            }
        }
    }

}