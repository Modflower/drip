# Drip

A configurable dripstone mod.

## How to Use?

Requirements:

- Minecraft 1.19+
- [Drip](https://github.com/KJP12/drip/releases)
- One of either mod loader:
	- [Quilt](https://quiltmc.org/install)
	- [Fabric](https://fabricmc.net/use)

Install Minecraft 1.19+, Drip and your mod loader of choice, then customise the config at `config/drip.json`. No client
mod is required, but you may want to install & configure for visual accuracy.

## Config - `drip.json`

*Example configuration*

```json5
{
	// Main entries of block -> {fluid, replace}
	"entries": {
		// Magma Block -> Lava
		"minecraft:magma_block": {
			// Sets the fluid to drip to lava.
			"fluid": "minecraft:lava",
			// Doesn't replace.
			"replace": false
		},
		// Ice -> Water
		"minecraft:ice": {
			// Sets the fluid to drip to water.
			"fluid": "minecraft:water",
			// Also doesn't replace.
			"replace": false
		}
	}
}
```
