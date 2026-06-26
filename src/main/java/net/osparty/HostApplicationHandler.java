package net.osparty;

import net.osparty.model.Activity;
import net.osparty.model.Applicant;
import java.util.List;

/**
 * Lets the host-facing UI mirror incoming applications in-game: an overlay
 * listing every pending applicant, and one-off chatbox pings as they arrive or
 * are resolved.
 */
public interface HostApplicationHandler
{
	/** Replace the in-game overlay with the current set of pending applicants. */
	void setPendingApplicants(List<Applicant> applicants, Activity activity);

	/** Chatbox ping for a newly-arrived applicant. */
	void announceApplicant(Applicant applicant, Activity activity);

	/** Chatbox ping after the host accepts or declines an applicant. */
	void announceResolved(Applicant applicant, Activity activity, boolean accepted);
}
