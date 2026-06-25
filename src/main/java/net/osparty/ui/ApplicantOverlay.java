package net.osparty.ui;

import net.osparty.model.Activity;
import net.osparty.model.Applicant;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.Map;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

/**
 * In-game overlay mimicking the host's view of an incoming applicant: their
 * combat stats and killcount for the activity (and its harder variant). Shown
 * while an application is pending; renders nothing otherwise.
 */
public class ApplicantOverlay extends OverlayPanel
{
	private volatile Applicant applicant;
	private volatile Activity activity;

	public ApplicantOverlay()
	{
		setPosition(OverlayPosition.TOP_LEFT);
	}

	public void setApplicant(Applicant applicant, Activity activity)
	{
		this.applicant = applicant;
		this.activity = activity;
	}

	public void clear()
	{
		this.applicant = null;
		this.activity = null;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		Applicant a = applicant;
		Activity act = activity;
		if (a == null || act == null)
		{
			return null;
		}

		panelComponent.getChildren().clear();
		panelComponent.setPreferredSize(new Dimension(150, 0));

		panelComponent.getChildren().add(TitleComponent.builder()
			.text("Party applicant")
			.color(Color.ORANGE)
			.build());

		panelComponent.getChildren().add(LineComponent.builder()
			.left(a.getName())
			.right("cb " + a.getCombatLevel())
			.build());

		if (a.getStats() != null)
		{
			for (Map.Entry<String, Integer> stat : a.getStats().entrySet())
			{
				panelComponent.getChildren().add(LineComponent.builder()
					.left(stat.getKey())
					.right(String.valueOf(stat.getValue()))
					.build());
			}
		}

		panelComponent.getChildren().add(LineComponent.builder()
			.left(act.getDisplayName() + " KC")
			.right(String.valueOf(a.getKillCount()))
			.rightColor(Color.GREEN)
			.build());

		if (act.hasHardMode() && a.getHardModeKillCount() >= 0)
		{
			panelComponent.getChildren().add(LineComponent.builder()
				.left(act.getHardModeLabel() + " KC")
				.right(String.valueOf(a.getHardModeKillCount()))
				.rightColor(Color.GREEN)
				.build());
		}

		return super.render(graphics);
	}
}
