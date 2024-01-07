package aplini.bedrockminerplugin;

import com.google.common.collect.ImmutableMap;
import net.coreprotect.CoreProtect;
import net.coreprotect.CoreProtectAPI;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.bukkit.Bukkit.getLogger;

public final class BedrockMinerPlugin extends JavaPlugin {



    @Override
    public void onEnable() {
        // 连接到 CoreProtect 插件
        CoreProtectAPI coreProtectAPI = null;
        Plugin coreProtect = getServer().getPluginManager().getPlugin("CoreProtect");
        if(!(coreProtect instanceof CoreProtect) || !coreProtect.isEnabled()){
            getLogger().info("未安装 CoreProtect 插件, 方块破坏不会被记录");
        }else{
            coreProtectAPI = ((CoreProtect) coreProtect).getAPI();
            getLogger().info("已连接到 CoreProtect 插件!");
        }
        // 注册监听器
        getServer().getPluginManager().registerEvents(new onPlayerInteractEvent(this, coreProtectAPI), this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}


// 监听玩家交互
class onPlayerInteractEvent implements Listener {
    private final BedrockMinerPlugin plugin;
    private final boolean EnableCoreProtect;
    private final CoreProtectAPI coreProtectAPI;
    private final List<Block> BlockList = new ArrayList<>();
    public onPlayerInteractEvent(BedrockMinerPlugin plugin, CoreProtectAPI coreProtectAPI) {
        this.plugin = plugin;
        this.coreProtectAPI = coreProtectAPI;
        EnableCoreProtect = coreProtectAPI != null;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {

        // 异步
        CompletableFuture.runAsync(() -> {

            // 要求点击的是基岩
            Block clickedBlock = event.getClickedBlock();
            if(clickedBlock == null || clickedBlock.getType() != Material.BEDROCK){
                return;
            }

            // 要求使用左键
            if(event.getAction() != Action.LEFT_CLICK_BLOCK){
                return;
            }

            // 玩家是否有权限
            Player player = event.getPlayer();
            if(!player.hasPermission("BedrockMinerPlugin.use")){
                return;
            }

            // 不是生存或冒险模式
            if(player.getGameMode() != GameMode.SURVIVAL && player.getGameMode() != GameMode.ADVENTURE){
                return;
            }

            // Lock :: 如果这个方块正在处理队列中
            if(BlockList.contains(clickedBlock)){
                return;
            }

            // 获取玩家库存
            PlayerInventory playerInventory = player.getInventory();

            // 要求玩家主手有下界合金镐
            ItemStack playerItemInMainHand = playerInventory.getItemInMainHand();
            if(playerItemInMainHand.getType() != Material.NETHERITE_PICKAXE){
                return;
            }

            // 要求下界合金镐有效率5附魔
            Map<Enchantment, Integer> enchants = playerItemInMainHand.getEnchantments();
            if(!enchants.containsKey(Enchantment.DIG_SPEED) || enchants.get(Enchantment.DIG_SPEED) != 5){
                return;
            }

            // 要求玩家有急迫2药水效果
            boolean beFastDigging2 = false;
            for(PotionEffect effect : player.getActivePotionEffects()){
                // 找到急迫2
                if(effect.getType().getName().equals("FAST_DIGGING") && effect.getAmplifier() == 1){
                    beFastDigging2 = true;
                }
            }
            if(!beFastDigging2){
                return;
            }

            // 要求玩家背包中有 活塞/ 红石火把/ 黏液块
            if(!playerInventory.contains(Material.PISTON, 10)){ // 活塞
                return;
            }
            if(!playerInventory.contains(Material.REDSTONE_TORCH, 10)){ // 红石火把
                return;
            }
            if(!playerInventory.contains(Material.SLIME_BLOCK, 10)){ // 黏液块
                return;
            }

            // 获取位置以及方块所在的世界
            Location location = clickedBlock.getLocation();
            World world = location.getWorld();
            if(world == null){
                return;
            }

            //
            // 检查结束
            //

            // 将这个基岩方块添加到处理队列中
            BlockList.add(clickedBlock);

            // 有 5% 的几率从背包中删除一个活塞
            if(new Random().nextInt(100) < 5){
                removePlayerInventoryItem(playerInventory, ImmutableMap.of(
                        Material.PISTON, 1
                ));
            }

            // 声音和粒子效果 //
            int delay = 0;

            // 声音 :: 放置石头
            Bukkit.getScheduler().runTaskLater(plugin, () -> world.playSound(location, Sound.BLOCK_STONE_PLACE, 1F, 1F), delay);

            // 声音 :: 放置石头
            delay += 20;
            Bukkit.getScheduler().runTaskLater(plugin, () -> world.playSound(location, Sound.BLOCK_STONE_PLACE, 1F, 1F), delay);

            delay += 20;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                // 声音 :: 放置石头
                world.playSound(location, Sound.BLOCK_STONE_PLACE, 1F, 1F);
                // 声音 :: 放置黏液块
                world.playSound(location, Sound.BLOCK_SLIME_BLOCK_PLACE, 1F, 1F);
                // 粒子效果 :: 黏液块
                location.add(0.5, 0.5, 0.5);
                world.spawnParticle(Particle.BLOCK_CRACK,
                        location, 40, 0.7, 0.7, 0.7, 2,
                        Material.SLIME_BLOCK.createBlockData());
            }, delay);

            // 声音 :: 放置石头
            delay += 20;
            Bukkit.getScheduler().runTaskLater(plugin, () -> world.playSound(location, Sound.BLOCK_STONE_PLACE, 1F, 1F), delay);

            delay += 27;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                // 声音 :: 放置石头
                world.playSound(location, Sound.BLOCK_STONE_PLACE, 1F, 1F);
                // 粒子效果 :: 红石火把
                world.spawnParticle(Particle.BLOCK_CRACK,
                        location, 20, 0.5, 0.5, 0.5, 2,
                        Material.REDSTONE_TORCH.createBlockData());
                // 声音 :: 放置石头
                world.playSound(location, Sound.BLOCK_STONE_PLACE, 1F, 1F);
            }, delay);

            delay += 52;
            location.add(0, 0.5, 0);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                // 粒子效果 :: 活塞
                world.spawnParticle(Particle.BLOCK_CRACK,
                        location, 80, 1, 1.5, 1, 2,
                        Material.PISTON.createBlockData());
                // 声音 :: 放置石头
                world.playSound(location, Sound.BLOCK_STONE_PLACE, 1F, 1F);
            }, delay);

            // 声音 :: 活塞推出
            delay += 34;
            Bukkit.getScheduler().runTaskLater(plugin, () -> world.playSound(location, Sound.BLOCK_PISTON_EXTEND, 1F, 1F), delay);

            // 声音 :: 活塞推动方块
            delay += 20;
            Bukkit.getScheduler().runTaskLater(plugin, () -> world.playSound(location, Sound.BLOCK_METAL_PLACE, 1F, 1F), delay);

            delay += 20;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                // 声音 :: 放置石头
                world.playSound(location, Sound.BLOCK_STONE_PLACE, 1F, 1F);
                // 粒子效果 :: 基岩
                world.spawnParticle(Particle.BLOCK_CRACK,
                        location, 40, 0.5, 0.5, 0.5, 2,
                        Material.BEDROCK.createBlockData());
            }, delay);

            // 声音 :: 破坏方块
            delay += 20;
            Bukkit.getScheduler().runTaskLater(plugin, () -> world.playSound(location, Sound.BLOCK_STONE_BREAK, 1F, 1F), delay);

            // 声音和粒子效果 END //

            // 在声音和粒子效果播放结束后运行破坏方块的代码
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                // 如果这个方块还是基岩
                if(clickedBlock.getType() == Material.BEDROCK){
                    clickedBlock.setType(Material.AIR);
                    // 使用 CoreProtect 记录这个方块被破坏
                    if(EnableCoreProtect && player.hasPermission("BedrockMinerPlugin.CoreProtect")){
                        coreProtectAPI.logRemoval(player.getName(), location, Material.BEDROCK, null);
                    }
                    // Lock :: 释放这个方块的锁
                    BlockList.remove(clickedBlock);
                }
            }, delay);
        });
    }


    // 从背包中移除物品
    public void removePlayerInventoryItem(PlayerInventory playerInventory, Map<Material, Integer> _delItems){

        Map<Material, Integer> delItems = new HashMap<>(_delItems);

        // 遍历库存
        for(int index = 0; index < playerInventory.getContents().length; index++){
            ItemStack item = playerInventory.getContents()[index];
            if(item != null){
                Material itemType = item.getType();
                // Map 中包含指定物品
                if(delItems.containsKey(itemType)){
                    // 检查这个 item 数量是否足够
                    int delQty = delItems.get(itemType);
                    int surplus = item.getAmount() - delQty;
                    // 足够, 减少物品数量
                    if(surplus >= 0){
                        item.setAmount(item.getAmount() - delQty);
                        if(item.getAmount() <= 0){
                            playerInventory.remove(item);
                        }
                        delItems.remove(itemType);
                    }
                    // 不足够, 删除后减少待删除数量
                    else {
                        playerInventory.remove(item);
                        delItems.put(itemType, delQty - surplus);
                    }

                    // 保存
                    playerInventory.setItem(index, item);

                    // 移除完成后跳出循环
                    if(delItems.isEmpty()){
                        break;
                    }
                }
            }
        }
    }

}