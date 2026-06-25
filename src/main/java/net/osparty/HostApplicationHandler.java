package net.osparty;

import net.osparty.model.Activity;
import net.osparty.model.Applicant;

/**
 * Lets the host-facing UI hand an incoming application to the plugin, which
 * mirrors it in-game (chatbox message + stats/killcount overlay) and clears it
 * once the host accepts or declines.
 */
public interface HostApplicationHandler
{
	/** A new applicant arrived for a party the player is hosting. */
	void onApplicantShown(Applicant applicant, Activity activity);

	/** The host accepted or declined the current applicant. */
	void onApplicantResolved(Applicant applicant, Activity activity, boolean accepted);
}
