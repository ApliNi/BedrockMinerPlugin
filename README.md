# BedrockMinerPlugin
适用于插件服务端的快速破基岩插件  

下载: https://modrinth.com/plugin/bedrockminerplugin

---

灵感来源于 [Fabric-Bedrock-Miner](https://github.com/LXYan2333/Fabric-Bedrock-Miner), 由于这个模组不能在一些特殊的服务器里使用而制作这个插件.  
该插件并不通过 "特性" 实现破坏基岩; 而是检查是否满足一些条件, 然后模拟出粒子和声音效果并删除基岩方块.  

从破坏方块开始, 到方块被破坏存在 4gt 的延迟.  


### 条件和特性
1. 生存或冒险模式
2. 手持效率5的下界合金镐
3. 急迫2药水效果 (来自信标)
4. 背包中有活塞, 红石火把, 黏液块. 每种物品至少10个

满足上述条件后左键点击基岩即可开始运行, 有 5% 几率扣除一个活塞作为 "损耗".  

---

### 连接到插件
- CoreProtect, 记录基岩方块被破坏


### 权限
```yaml
permissions:

  BedrockMinerPlugin.use:
    description: 允许使用此插件进行破基岩
    default: true

  BedrockMinerPlugin.CoreProtect:
    description: 允许通过 CoreProtect 插件记录方块被破坏
    default: true
```
