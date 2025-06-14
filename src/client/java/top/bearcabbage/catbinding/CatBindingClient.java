package top.bearcabbage.catbinding;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.text.LiteralTextContent;
import net.minecraft.text.MutableText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;

import java.util.Optional;

public class CatBindingClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// This entrypoint is suitable for setting up client-specific logic, such as rendering.
	}

	public static MutableText extractBeforeTarget(Text root, String targetLiteral) {
		StringBuilder sb = new StringBuilder();
		boolean[] foundTarget = {false}; // 使用数组以便在 lambda 中修改

		// 遍历文本节点
		root.visit((StringVisitable.Visitor<Optional<Boolean>>) text -> {
			if (text.equals(targetLiteral)) {
				foundTarget[0] = true; // 标记已找到
				return Optional.of(Optional.of(true)); // 停止遍历
			} else {
				sb.append(text);
				return Optional.empty();
			}
		});

		// 如果没找到目标文本，返回原始 Text，否则返回构造的 LiteralText
		if (!foundTarget[0]) {
			return (MutableText) root;
		} else {
			return MutableText.of(new LiteralTextContent(sb.toString()));
		}
	}
}