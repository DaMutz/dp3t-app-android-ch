package org.dpppt.android.app.debug;

import org.dpppt.android.app.debug.model.DebugAppState;
import org.dpppt.android.app.main.model.AppState;
import org.dpppt.android.app.main.model.NotificationState;
import org.dpppt.android.app.main.model.TracingState;
import org.dpppt.android.app.main.model.TracingStatusInterface;
import org.dpppt.android.sdk.InfectionStatus;
import org.dpppt.android.sdk.TracingStatus;

public class TracingStatusWrapper implements TracingStatusInterface {

	private DebugAppState debugAppState = DebugAppState.NONE;
	private TracingStatus status;

	public TracingStatusWrapper(DebugAppState debugAppState) {
		this.debugAppState = debugAppState;
	}

	public void setStatus(TracingStatus status) {
		this.status = status;
	}

	@Override
	public boolean isReportedAsInfected() {
		return status.getInfectionStatus() == InfectionStatus.INFECTED || debugAppState == DebugAppState.REPORTED_EXPOSED;
	}

	@Override
	public boolean wasContactReportedAsExposed() {
		return status.getInfectionStatus() == InfectionStatus.EXPOSED || debugAppState == DebugAppState.CONTACT_EXPOSED;
	}

	@Override
	public void setDebugAppState(DebugAppState debugAppState) {
		this.debugAppState = debugAppState;
	}

	@Override
	public DebugAppState getDebugAppState() {
		return debugAppState;
	}

	@Override
	public AppState getAppState() {
		boolean hasError = status.getErrors().size() > 0;
		boolean tracingOff = !(status.isAdvertising() || status.isReceiving());
		switch (debugAppState) {
			case NONE:
				if (isReportedAsInfected() || wasContactReportedAsExposed()) {
					return AppState.EXPOSED;
				} else if (tracingOff) {
					return AppState.TRACING_OFF;
				} else if (hasError) {
					return getAppStateForError(status.getErrors().iterator().next());
				} else {
					return AppState.TRACING_ON;
				}
			case HEALTHY:
				if (tracingOff) {
					return AppState.TRACING_OFF;
				} else if (hasError) {
					return getAppStateForError(status.getErrors().iterator().next());
				} else {
					return AppState.TRACING_ON;
				}
			case REPORTED_EXPOSED:
			case CONTACT_EXPOSED:
				return AppState.EXPOSED;
		}
		throw new IllegalStateException("Unkown debug AppState: " + debugAppState.toString());
	}

	private AppState getAppStateForError(TracingStatus.ErrorState error) {
		switch (error) {
			case BLE_DISABLED:
				return AppState.ERROR_BLUETOOTH_OFF;
			case MISSING_LOCATION_PERMISSION:
				return AppState.ERROR_LOCATION_PERMISSION;
			case BATTERY_OPTIMIZER_ENABLED:
				return AppState.ERROR_BATTERY_OPTIMIZATION;
			case NETWORK_ERROR_WHILE_SYNCING:
				return AppState.ERROR_SYNC_FAILED;
		}
		throw new IllegalStateException("Unkown ErrorState: " + error.toString());
	}

	@Override
	public TracingState getTracingState() {
		boolean tracingOff = !(status.isAdvertising() || status.isReceiving());
		return tracingOff ? TracingState.NOT_ACTIVE : TracingState.ACTIVE;
	}

	@Override
	public TracingStatus.ErrorState getTracingErrorState() {
		boolean hasError = status.getErrors().size() > 0;
		if (hasError) {
			return status.getErrors().iterator().next();
		}
		throw new IllegalStateException("Should not call function if there is no error: ");
	}

	@Override
	public NotificationState getNotificationState() {
		switch (debugAppState) {
			case NONE:
				if (isReportedAsInfected()) {
					return NotificationState.POSITIVE_TESTED;
				} else if (wasContactReportedAsExposed()) {
					return NotificationState.EXPOSED;
				} else {
					return NotificationState.NO_REPORTS;
				}
			case HEALTHY:
				return NotificationState.NO_REPORTS;
			case REPORTED_EXPOSED:
				return NotificationState.POSITIVE_TESTED;
			case CONTACT_EXPOSED:
				return NotificationState.EXPOSED;
		}
		throw new IllegalStateException("Unkown debug AppState: " + debugAppState.toString());
	}

}
