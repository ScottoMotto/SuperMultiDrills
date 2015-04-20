package net.silentchaos512.supermultidrills.item;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import org.lwjgl.input.Keyboard;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemDye;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemTool;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.oredict.OreDictionary;
import net.silentchaos512.supermultidrills.SuperMultiDrills;
import net.silentchaos512.supermultidrills.configuration.Config;
import net.silentchaos512.supermultidrills.lib.EnumDrillMaterial;
import net.silentchaos512.supermultidrills.lib.Names;
import net.silentchaos512.supermultidrills.registry.IAddRecipe;
import net.silentchaos512.supermultidrills.util.LocalizationHelper;
import net.silentchaos512.supermultidrills.util.LogHelper;
import cofh.api.energy.IEnergyContainerItem;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.udojava.evalex.Expression;

import cpw.mods.fml.common.registry.GameRegistry;

public class Drill extends ItemTool implements IAddRecipe, IEnergyContainerItem {

  /*
   * Effective materials
   */
  private static final Set effectiveMaterialsBasic = Sets.newHashSet(new Material[] {
      Material.anvil, Material.circuits, Material.clay, Material.glass, Material.grass,
      Material.ground, Material.ice, Material.iron, Material.packedIce, Material.piston,
      Material.rock, Material.sand, Material.snow });
  private static final Set effectiveMaterialsExtra = Sets.newHashSet(new Material[] {
      Material.cloth, Material.gourd, Material.leaves, Material.plants, Material.vine,
      Material.web, Material.wood });

  // public static final int MAX_ENERGY_IO = 10000;

  /*
   * Render pass Ids
   */
  public static final int PASS_CHASSIS = 0;
  public static final int PASS_HEAD = 1;
  public static final int NUM_RENDER_PASSES = 2;

  /*
   * NBT keys
   */
  public static final String NBT_HEAD = "Head";
  public static final String NBT_MOTOR = "Motor";
  public static final String NBT_BATTERY = "Battery";
  public static final String NBT_CHASSIS = "Chassis";
  public static final String NBT_ENERGY = "Energy";
  public static final String NBT_SAW = "Saw";
  // public static final String NBT_SILK = "Silk";
  // public static final String NBT_SPEED = "Speed";

  /*
   * The basic recipe that shows up in NEI. It's referenced in RecipeCraftDrill, but the crafting result is overridden
   * there.
   */
  public static IRecipe baseRecipe;

  public Drill() {

    // The values passed into super should have no effect.
    super(4.0f, ToolMaterial.EMERALD, effectiveMaterialsBasic);
    this.setMaxStackSize(1);
    this.setCreativeTab(SuperMultiDrills.creativeTab);
  }

  @Override
  public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean advanced) {
    
    boolean shifted = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)
        || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);

    if (stack.stackTagCompound != null && !stack.stackTagCompound.hasKey(NBT_HEAD)) {
      int i = 1;
      String itemName = Names.DRILL;
      String s = LocalizationHelper.getItemDescription(itemName, i);
      while (!s.equals(LocalizationHelper.getItemDescriptionKey(itemName, i)) && i < 8) {
        list.add(EnumChatFormatting.DARK_GRAY + s);
        s = LocalizationHelper.getItemDescription(itemName, ++i);
      }

      if (i == 1) {
        s = LocalizationHelper.getItemDescription(itemName, 0);
        if (!s.equals(LocalizationHelper.getItemDescriptionKey(itemName, 0))) {
          list.add(EnumChatFormatting.DARK_GRAY
              + LocalizationHelper.getItemDescription(itemName, 0));
        }
      }
    } else {
      int energy = this.getEnergyStored(stack);
      int energyMax = this.getMaxEnergyStored(stack);
      String str = EnumChatFormatting.YELLOW + String.format("%d / %d", energy, energyMax);
      list.add(str);
      
      if (shifted) {
        list.add("Mining level: " + this.getHarvestLevel(stack, ""));
        list.add("Energy cost: " + this.getEnergyToBreakBlock(stack, 1.0f));
      }
    }
  }

  @Override
  public void addRecipes() {

    ItemStack head = new ItemStack(ModItems.drillHead, 1, OreDictionary.WILDCARD_VALUE);
    ItemStack motor = new ItemStack(ModItems.drillMotor, 1, OreDictionary.WILDCARD_VALUE);
    ItemStack chassis = new ItemStack(ModItems.drillChassis, 1, OreDictionary.WILDCARD_VALUE);
    ItemStack battery = new ItemStack(ModItems.drillBattery, 1, OreDictionary.WILDCARD_VALUE);
    // This recipe is only for NEI! It will be overridden by a custom recipe handler.
    baseRecipe = GameRegistry.addShapedRecipe(new ItemStack(this), "  h", " m ", "cb ", 'h', head,
        'm', motor, 'c', chassis, 'b', battery);
  }

  @Override
  public void addOreDict() {

  }

  public EnumDrillMaterial getDrillMaterial(ItemStack stack) {

    int headId = this.getTag(stack, NBT_HEAD);
    if (headId < 0 || headId >= EnumDrillMaterial.values().length) {
      headId = 0;
    }
    return EnumDrillMaterial.values()[headId];
  }

  public float getDigSpeed(ItemStack stack) {

    // TODO: Speed modifiers?
    return this.getDrillMaterial(stack).getEfficiency();
  }

  public int getEnergyToBreakBlock(ItemStack stack, float hardness) {

    int efficiencyLevel = EnchantmentHelper.getEnchantmentLevel(Enchantment.efficiency.effectId,
        stack);
    EnumDrillMaterial material = this.getDrillMaterial(stack);
    
    Expression exp = Config.energyCostExpression;
    exp.setVariable("durability", BigDecimal.valueOf(material.getDurability()));
    exp.setVariable("efficiency", BigDecimal.valueOf(efficiencyLevel));
    exp.setVariable("hardness", BigDecimal.valueOf(hardness));
    exp.setVariable("mining_speed", BigDecimal.valueOf(material.getEfficiency()));
    return exp.eval().intValue();
  }

  public int getTag(ItemStack stack, String key) {

    if (stack.stackTagCompound == null) {
      stack.setTagCompound(new NBTTagCompound());
    }
    if (stack.stackTagCompound.hasKey(key)) {
      return stack.stackTagCompound.getInteger(key);
    } else {
      return 0;
    }
  }

  public boolean getTagBoolean(ItemStack stack, String key) {

    if (stack.stackTagCompound == null) {
      stack.setTagCompound(new NBTTagCompound());
    }
    if (stack.stackTagCompound.hasKey(key)) {
      return stack.stackTagCompound.getBoolean(key);
    } else {
      return false;
    }
  }

  public void setTag(ItemStack stack, String key, int value) {

    if (stack.stackTagCompound == null) {
      stack.setTagCompound(new NBTTagCompound());
    }
    stack.stackTagCompound.setInteger(key, value);
  }

  public void setTagBoolean(ItemStack stack, String key, boolean value) {

    if (stack.stackTagCompound == null) {
      stack.setTagCompound(new NBTTagCompound());
    }
    stack.stackTagCompound.setBoolean(key, value);
  }

  @Override
  public int getHarvestLevel(ItemStack stack, String toolClass) {

    int motorLevel = this.getTag(stack, NBT_MOTOR);
    // if (motorLevel < 0) {
    // motorLevel = 0;
    // }
    // return motorLevel + 2;
    switch (motorLevel) {
      case 2:
        return Config.motor2Level;
      case 1:
        return Config.motor1Level;
      default:
        return Config.motor0Level;
    }
  }

  @Override
  public Set getToolClasses(ItemStack stack) {

    boolean hasSaw = this.getTagBoolean(stack, NBT_SAW);
    // return ImmutableSet.of("pickaxe");
    if (hasSaw) {
      return ImmutableSet.of("pickaxe", "shovel", "axe");
    } else {
      return ImmutableSet.of("pickaxe", "shovel");
    }
  }

  // Can harvest block? Direct copy from ItemPickaxe.
  public boolean func_150897_b(Block block) {

    return block == Blocks.obsidian ? this.toolMaterial.getHarvestLevel() == 3
        : (block != Blocks.diamond_block && block != Blocks.diamond_ore ? (block != Blocks.emerald_ore
            && block != Blocks.emerald_block ? (block != Blocks.gold_block
            && block != Blocks.gold_ore ? (block != Blocks.iron_block && block != Blocks.iron_ore ? (block != Blocks.lapis_block
            && block != Blocks.lapis_ore ? (block != Blocks.redstone_ore
            && block != Blocks.lit_redstone_ore ? (block.getMaterial() == Material.rock ? true
            : (block.getMaterial() == Material.iron ? true : block.getMaterial() == Material.anvil))
            : this.toolMaterial.getHarvestLevel() >= 2)
            : this.toolMaterial.getHarvestLevel() >= 1)
            : this.toolMaterial.getHarvestLevel() >= 1)
            : this.toolMaterial.getHarvestLevel() >= 2)
            : this.toolMaterial.getHarvestLevel() >= 2)
            : this.toolMaterial.getHarvestLevel() >= 2);
  }

  // Efficiency vs block?
  @Override
  public float func_150893_a(ItemStack stack, Block block) {

    boolean isEffective = effectiveMaterialsBasic.contains(block.getMaterial());
    if (!isEffective && this.getTagBoolean(stack, NBT_SAW)) {
      isEffective = effectiveMaterialsExtra.contains(block.getMaterial());
    }

    if (isEffective && this.getEnergyStored(stack) > 0) {
      return this.getDigSpeed(stack);
    }

    return 1.0f;
  }

  @Override
  public int getItemEnchantability(ItemStack stack) {

    return 0;
  }

  @Override
  public boolean hasEffect(ItemStack stack) {

    return false;
  }

  @Override
  public boolean hitEntity(ItemStack stack, EntityLivingBase entity1, EntityLivingBase entity2) {

    // TODO: Draw power?
    return true;
  }

  @Override
  public boolean onBlockDestroyed(ItemStack stack, World world, Block block, int x, int y, int z,
      EntityLivingBase entity) {

    float hardness = block.getBlockHardness(world, x, y, z);
    if (hardness != 0.0f) {
      int cost = this.getEnergyToBreakBlock(stack, hardness);
      if (Config.printMiningCost) {
        LogHelper.debug(cost);
      }
      this.extractEnergy(stack, cost, false);
    }

    return true;
  }

  @Override
  public boolean isFull3D() {

    return true;
  }

  @Override
  public Multimap getAttributeModifiers(ItemStack stack) {

    Multimap multimap = super.getAttributeModifiers(stack);
    double damage = this.getDrillMaterial(stack).getDamageVsEntity();
    multimap.put(SharedMonsterAttributes.attackDamage.getAttributeUnlocalizedName(),
        new AttributeModifier(field_111210_e, "Weapon modifier", damage, 0));
    return multimap;
  }

  public int getMaxEnergyExtracted(ItemStack container) {

    return 1000000;
  }

  public int getMaxEnergyReceived(ItemStack container) {

    return this.getMaxEnergyStored(container) / 100;
  }

  @Override
  public int receiveEnergy(ItemStack container, int maxReceive, boolean simulate) {

    int energy = getEnergyStored(container);
    int energyReceived = Math.min(getMaxEnergyStored(container) - energy,
        Math.min(this.getMaxEnergyReceived(container), maxReceive));

    if (!simulate) {
      energy += energyReceived;
      this.setTag(container, NBT_ENERGY, energy);
    }
    return energyReceived;
  }

  @Override
  public int extractEnergy(ItemStack container, int maxExtract, boolean simulate) {

    if (container.stackTagCompound == null || !container.stackTagCompound.hasKey(NBT_ENERGY)) {
      return 0;
    }
    int energy = getEnergyStored(container);
    int energyExtracted = Math.min(energy,
        Math.min(this.getMaxEnergyExtracted(container), maxExtract));

    if (!simulate) {
      energy -= energyExtracted;
      this.setTag(container, NBT_ENERGY, energy);
    }
    return energyExtracted;
  }

  @Override
  public int getEnergyStored(ItemStack container) {

    return this.getTag(container, NBT_ENERGY);
  }

  @Override
  public int getMaxEnergyStored(ItemStack container) {

    int battery = this.getTag(container, NBT_BATTERY);
    // LogHelper.debug(battery);
    switch (battery) {
      case 4:
        // LogHelper.debug(Config.battery4MaxCharge);
        return Config.battery4MaxCharge;
      case 3:
        return Config.battery3MaxCharge;
      case 2:
        return Config.battery2MaxCharge;
      case 1:
        return Config.battery1MaxCharge;
      default:
        return Config.battery0MaxCharge;
    }
  }

  @Override
  public String getUnlocalizedName(ItemStack stack) {

    return LocalizationHelper.ITEM_PREFIX + Names.DRILL;
  }

  @Override
  public IIcon getIcon(ItemStack stack, int pass) {

    if (pass == PASS_CHASSIS) {
      // Chassis
      return ModItems.drillChassis.getIconFromDamage(0);
    } else if (pass == PASS_HEAD) {
      // Head
      int head = this.getTag(stack, NBT_HEAD);
      if (head < 0 || head >= ModItems.drillHead.icons.length) {
        head = 0;
      }
      return ModItems.drillHead.icons[head];
    } else {
      return null;
    }
  }

  @Override
  public int getColorFromItemStack(ItemStack stack, int pass) {

    if (pass == PASS_CHASSIS) {
      int color = this.getTag(stack, NBT_CHASSIS);
      return ItemDye.field_150922_c[~color & 15];
    } else {
      return 0xFFFFFF;
    }
  }

  @Override
  public int getRenderPasses(int meta) {

    return NUM_RENDER_PASSES;
  }

  @Override
  public boolean requiresMultipleRenderPasses() {

    return true;
  }

  @Override
  public boolean showDurabilityBar(ItemStack stack) {

    return stack.stackTagCompound != null && stack.stackTagCompound.hasKey(NBT_ENERGY);
  }

  @Override
  public double getDurabilityForDisplay(ItemStack stack) {

    int energy = this.getEnergyStored(stack);
    int energyMax = this.getMaxEnergyStored(stack);
    return (double) (energyMax - energy) / (double) energyMax;
  }

  @Override
  public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z,
      int side, float hitX, float hitY, float hitZ) {

    boolean used = false;
    int toolSlot = player.inventory.currentItem;
    int itemSlot = toolSlot + 1;
    ItemStack nextStack = null;

    if (toolSlot < 8) {
      nextStack = player.inventory.getStackInSlot(itemSlot);
      if (nextStack != null) {
        Item item = nextStack.getItem();
        Item bandolier = (Item) Item.itemRegistry.getObject("SilentGems:TorchBandolier");
        if (item instanceof ItemBlock || (bandolier != null && item == bandolier)) {
          ForgeDirection d = ForgeDirection.VALID_DIRECTIONS[side];

          int px = x + d.offsetX;
          int py = y + d.offsetY;
          int pz = z + d.offsetZ;
          int playerX = (int) Math.floor(player.posX);
          int playerY = (int) Math.floor(player.posY);
          int playerZ = (int) Math.floor(player.posZ);

          // Check for overlap with player, except for torches and torch bandolier
          if (Item.getIdFromItem(item) != Block.getIdFromBlock(Blocks.torch)
              && !(bandolier != null && item == bandolier) && px == playerX
              && (py == playerY || py == playerY + 1 || py == playerY - 1) && pz == playerZ) {
            return false;
          }

          used = item.onItemUse(nextStack, player, world, x, y, z, side, hitX, hitY, hitZ);
          if (nextStack.stackSize < 1) {
            nextStack = null;
            player.inventory.setInventorySlotContents(itemSlot, null);
          }
        }
      }
    }

    return used;
  }
}
