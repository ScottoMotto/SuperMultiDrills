package net.silentchaos512.supermultidrills.recipe;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.init.Enchantments;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.silentchaos512.lib.util.DyeHelper;
import net.silentchaos512.supermultidrills.SuperMultiDrills;
import net.silentchaos512.supermultidrills.item.Drill;
import net.silentchaos512.supermultidrills.item.DrillBattery;
import net.silentchaos512.supermultidrills.item.DrillChassis;
import net.silentchaos512.supermultidrills.item.DrillHead;
import net.silentchaos512.supermultidrills.item.DrillMotor;
import net.silentchaos512.supermultidrills.item.DrillUpgrade;
import net.silentchaos512.supermultidrills.item.ModItems;
import net.silentchaos512.supermultidrills.lib.Names;
import net.silentchaos512.supermultidrills.util.InventoryHelper;

public class RecipeUpgradeDrill implements IRecipe {

  @Override
  public boolean matches(InventoryCrafting inv, World world) {

    int countDrill = 0;
    int countBattery = 0;
    int countChassis = 0;
    int countHead = 0;
    int countHeadMaterial = 0;
    int countMotor = 0;
    int countDye = 0;
    ItemStack stack;

    for (int i = 0; i < inv.getSizeInventory(); ++i) {
      stack = inv.getStackInSlot(i);
      if (stack != null) {
        // Dye
        boolean isDye = DyeHelper.isItemDye(stack);

        Item item = stack.getItem();
        if (item instanceof Drill) {
          ++countDrill;
        } else if (item instanceof DrillBattery) {
          ++countBattery;
        } else if (item instanceof DrillChassis) {
          ++countChassis;
        } else if (item instanceof DrillHead) {
          ++countHead;
        } else if (item instanceof DrillMotor) {
          ++countMotor;
        } else if (item instanceof DrillUpgrade) {
          ;
        } else if (isDye) {
          ++countDye;
        } else if (InventoryHelper.isDrillHeadMaterial(stack)) {
          ++countHeadMaterial;
        } else {
          return false;
        }
      }
    }

    // return countDrill == 1 && countUpgrade >= 1;
    boolean flagColor = countChassis <= 1 && countDye <= 1 && !(countChassis == 1 && countDye == 1);
    return countDrill == 1 && countBattery <= 1 && countHead <= 1 && countHeadMaterial <= 1
        && countMotor <= 1 && flagColor;
  }

  @Override
  public ItemStack getCraftingResult(InventoryCrafting inv) {

    ItemStack stack;
    ItemStack drill = null;

    // Find the drill
    for (int i = 0; i < inv.getSizeInventory(); ++i) {
      stack = inv.getStackInSlot(i);
      if (stack != null && stack.getItem() instanceof Drill) {
        drill = stack;
        break;
      }
    }

    // Did we get a drill?
    if (drill == null) {
      return null;
    }

    // Copy the drill, we can't modify the original!
    ItemStack result = drill.copy();

    // Find and apply all upgrades
    Item item;
    for (int i = 0; i < inv.getSizeInventory(); ++i) {
      stack = inv.getStackInSlot(i);
      if (stack != null) {
        item = stack.getItem();
        if (item instanceof DrillUpgrade) {
          // Upgrades
          result = applyDrillUpgrade(result, stack);
        } else if (item instanceof DrillBattery) {
          // Battery change
          // Remove next 2 statements!
          ItemStack oldBattery = new ItemStack(ModItems.drillBattery, 1,
              ModItems.drill.getTag(result, Drill.NBT_BATTERY));
          ModItems.drillBattery.setTag(oldBattery, ModItems.drillBattery.NBT_ENERGY,
              ModItems.drill.getEnergyStored(result));

          ModItems.drill.setTag(result, Drill.NBT_BATTERY, stack.getItemDamage());
          ModItems.drill.setTag(result, Drill.NBT_ENERGY,
              ModItems.drillBattery.getEnergyStored(stack));
          // Cap to new max charge
          int maxEnergy = ModItems.drill.getMaxEnergyStored(result);
          if (ModItems.drill.getEnergyStored(result) > maxEnergy) {
            ModItems.drill.setTag(result, Drill.NBT_ENERGY, maxEnergy);
          }
        } else if (item instanceof DrillChassis) {
          // Chassis change (same as dyeing)
          ModItems.drill.setTag(result, Drill.NBT_CHASSIS, ~stack.getItemDamage() & 15);
        } else if (DyeHelper.isItemDye(stack)) {
          // Dye the chassis
          int value = ~DyeHelper.oreDictDyeToVanilla(stack).getItemDamage() & 15;
          ModItems.drill.setTag(result, Drill.NBT_CHASSIS, value);
        } else if (item instanceof DrillHead) {
          // Head change
          ModItems.drill.setTag(result, Drill.NBT_HEAD, stack.getItemDamage());
        } else if (item instanceof DrillMotor) {
          // Motor change
          ModItems.drill.setTag(result, Drill.NBT_MOTOR, stack.getItemDamage());
        } else if (InventoryHelper.isDrillHeadMaterial(stack)) {
          int id = InventoryHelper.getDrillHeadMaterialId(stack);
          ModItems.drill.setTag(result, Drill.NBT_HEAD_COAT, id);
        }
      }
    }

    return result;
  }

  private ItemStack applyDrillUpgrade(ItemStack drill, ItemStack upgrade) {

    // If the upgrade cannot be applied, null will be returned, so this could happen.
    if (drill == null) {
      return null;
    }

    // This shouldn't happen, but you can never check for null too often, right?
    if (upgrade == null) {
      SuperMultiDrills.logHelper.warning("RecipeUpgradeDrill.applyDrillUpgrade: upgrade is null!");
      return null;
    }

    int meta = upgrade.getItemDamage();
    if (meta == ModItems.drillUpgrade.getMetaFor(Names.UPGRADE_SAW)) {
      // Saw
      if (ModItems.drill.getTagBoolean(drill, Drill.NBT_SAW)) {
        return null;
      }
      ModItems.drill.setTagBoolean(drill, Drill.NBT_SAW, true);
    } else if (meta == ModItems.drillUpgrade.getMetaFor(Names.UPGRADE_FORTUNE)) {
      // Fortune
      if (EnchantmentHelper.getEnchantmentLevel(Enchantments.FORTUNE, drill) > 0) {
        return null;
      }
      return this.increaseEnchantmentLevel(drill, Enchantments.FORTUNE, 3);
    } else if (meta == ModItems.drillUpgrade.getMetaFor(Names.UPGRADE_SILK)) {
      // Silk
      if (EnchantmentHelper.getEnchantmentLevel(Enchantments.SILK_TOUCH, drill) > 0) {
        return null;
      }
      return this.increaseEnchantmentLevel(drill, Enchantments.SILK_TOUCH, 1);
    } else if (meta == ModItems.drillUpgrade.getMetaFor(Names.UPGRADE_SPEED)) {
      // Speed
      return this.increaseEnchantmentLevel(drill, Enchantments.EFFICIENCY, 5);
    } else if (meta == ModItems.drillUpgrade.getMetaFor(Names.UPGRADE_SHARPNESS)) {
      // Sharpness
      return increaseEnchantmentLevel(drill, Enchantments.SHARPNESS, 5);
    } else if (meta == ModItems.drillUpgrade.getMetaFor(Names.UPGRADE_AREA_MINER)) {
      // Area Miner
      if (ModItems.drill.getTagBoolean(drill, Drill.NBT_AREA_MINER)) {
        return null;
      }
      ModItems.drill.setTagBoolean(drill, Drill.NBT_AREA_MINER, true);
    } else if (meta == ModItems.drillUpgrade.getMetaFor(Names.UPGRADE_GRAVITON_GENERATOR)) {
      // Graviton Generator
      if (ModItems.drill.getTagBoolean(drill, Drill.NBT_GRAVITON_GENERATOR)) {
        return null;
      }
      ModItems.drill.setTagBoolean(drill, Drill.NBT_GRAVITON_GENERATOR, true);
    }

    return drill;
  }

  private ItemStack increaseEnchantmentLevel(ItemStack drill, Enchantment enchantment,
      int maxLevel) {

    int level = EnchantmentHelper.getEnchantmentLevel(enchantment, drill);
    if (level == 0) {
      drill.addEnchantment(enchantment, 1);
    } else if (level < maxLevel) {
      NBTTagCompound tagCompound;
      for (int i = 0; i < drill.getEnchantmentTagList().tagCount(); ++i) {
        tagCompound = (NBTTagCompound) drill.getEnchantmentTagList().getCompoundTagAt(i);
        int id = tagCompound.getShort("id");
        if (id == Enchantment.getEnchantmentID(enchantment)) {
          tagCompound.setShort("lvl", (short) (level + 1));
        }
      }
    } else {
      return null;
    }
    return drill;
  }

  @Override
  public int getRecipeSize() {

    return 9;
  }

  @Override
  public ItemStack getRecipeOutput() {

    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ItemStack[] getRemainingItems(InventoryCrafting inv) {

    for (int i = 0; i < inv.getSizeInventory(); ++i) {
      ItemStack stack = inv.getStackInSlot(i);
      if (stack != null) {
        --stack.stackSize;
        if (stack.stackSize <= 0) {
          stack = null;
        }
        inv.setInventorySlotContents(i, stack);
      }
    }
    return new ItemStack[] {};
  }
}
