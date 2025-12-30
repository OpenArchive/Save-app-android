UploadService review (JobService + scheduling + DI)

1) Scheduling API + entry points

Issues
•	You have 3 startUploadService(...) overloads, including one that pulls Context from GlobalContext. That’s a smell:
•	hard to test
•	easy to call from anywhere (including Composables) and accidentally spam schedules
•	ties a system concern to Koin global state

Best practice
•	Replace companion “start” functions with a single injected scheduler (UploadJobScheduler) used by ViewModels / use-cases.
•	Use applicationContext only.

Also important
•	MY_BACKGROUND_JOB = 0 is risky. ID 0 can collide with other jobs in your app or libraries if not careful. Pick a high stable constant and reserve a range (or define centrally).

⸻

2) DI inside JobService

Issues
•	private val context: Context by inject() is wrong/unnecessary. A JobService is already a Context.
•	DI in Service is OK, but prefer injecting pure collaborators:
•	AnalyticsManager
•	UploadCoordinator / repository / DB access layer
•	maybe a NetworkChecker

Best practice
•	Remove context injection.
•	Keep analytics injection, but try to move “orchestration” out of the service into a testable class.

⸻

3) Coroutine lifecycle + cancellation (critical)

Right now you do:

CoroutineScope(Dispatchers.IO).launch { ... }

This scope is not tied to the service lifecycle, so:
•	onStopJob() is called → you set mKeepUploading=false and cancel conduits
•	but any in-flight coroutine might continue (especially if conduit blocks)
•	OS believes job stopped, but your work might still run = bad

Fix
Use a service-owned SupervisorJob and cancel it in onStopJob().

Also: mKeepUploading is never reset to true in onStartJob() / before running. Once a stop happens, the next run will immediately skip the loop. That’s a real bug.

⸻

4) How many times is onStartJob called? duplicates? instances?

Key facts
•	JobService instances: The system can create a new instance when your process is (re)started. You cannot assume singleton.
•	onStartJob can be called multiple times over app lifetime:
•	every time you schedule that job and constraints are met
•	after reschedule (because you return true from onStopJob)
•	after network/charging changes (if constraints require them)
•	Duplicate running:
•	JobScheduler generally runs one job per jobId at a time, but you can still get overlapping behavior if:
•	you schedule again while a previous run is still active
•	your previous run didn’t actually stop (due to bad cancellation)
•	you have multiple processes (rare, but possible)
•	Your mRunning flag does not protect across process restarts and is not thread-safe. It only guards within that service instance.

Recommendation
•	Guard concurrency with a persistent/atomic mechanism:
•	simplest: a DB “upload_lock” row / “isUploading” state in DB, or
•	a Mutex in a singleton UploadCoordinator (still not process-proof), plus
•	correct cancellation so “overlap” is minimized
•	Make the job idempotent: it should be safe if it starts twice.

⸻

5) JobFinished / reschedule correctness

In onStopJob() you return true always → “reschedule”.
That can create aggressive retry loops (especially if user cancels or you have a permanent error).

Best practice
•	Return true only for transient failures (network lost, server 5xx).
•	Return false for user-cancel or permanent failures.
•	In onStartJob, when you exit early due to no work, call jobFinished(params, false) quickly.

⸻

6) Network logic (shouldUpload) is doing too much (and can cause loops)

You already schedule job with .setRequiredNetworkType(...) in startUploadService.
Then inside shouldUpload() you:
•	check network yourself
•	and if not available, schedule another job again with a network constraint

This has a few problems:
•	scheduling from within the job can spam schedule()
•	you’re duplicating what JobScheduler already provides: “run when network returns”

Better model
•	Decide required network type at schedule time:
•	wifi-only → NETWORK_TYPE_UNMETERED
•	otherwise → NETWORK_TYPE_ANY
•	Inside the job, don’t reschedule manually. If network isn’t there anyway, you can just jobFinished(params, true) or exit (but ideally it shouldn’t start if constraints are correct).

Also: your isNetworkAvailable() uses activeNetwork only. That’s OK for a quick gate, but don’t rely on it for scheduling decisions.

⸻

7) Upload loop logic (DB + statuses) review

Good
•	You use Queued + Uploading to resume.
•	You emit progress and state updates through UploadEventBus and Broadcast.

Concerns / improvements
1.	Resetting progress to 0:
•	you do it when status != Uploading. That’s fine if you treat a job run as a “session”.
•	but for resumable uploads, resetting can confuse UI.
•	If you support chunking/resume, keep previous progress unless you truly restart from byte 0.
2.	Media.getByStatus(...) inside while loop:
•	you re-query DB each iteration. That’s OK, but watch DB churn.
•	consider fetching a batch and only refetch when it’s empty or after N items.
3.	mKeepUploading bug:
•	must reset to true when a new job starts.
4.	error handling
•	You catch IOException around upload(media) but conduits throw many other exceptions (RuntimeException in IA, etc.). In WebDavConduit you already catch Throwable and call jobFailed.
•	In UploadService, catching only IOException is inconsistent. You should catch Throwable once at the outer per-media boundary and map error types consistently.
5.	backoff
•	Today: immediate loop across all queued items. If server is down, you’ll hammer.
•	Add a transient failure strategy:
•	if n consecutive network/server failures → stop + reschedule with backoff (JobScheduler backoff isn’t available like WorkManager; you can implement delay or just reschedule later).

⸻

8) Notifications (Android 14+)

You set setNotification(...) only on API 34+. That’s good for user-initiated jobs.
But note:
•	You should also consider showing a normal persistent “Uploading…” notification for long sessions even pre-34 if UX expects “uploads in background”. Otherwise process may be killed more.

⸻

9) Remove WorkManager Configuration line

This in onCreate() is a no-op:

Configuration.Builder().setJobSchedulerJobIdRange(0, Integer.MAX_VALUE).build()

If you use WorkManager, configure it in Application. If not, remove.

⸻

Conduit architecture review (IaConduit + WebDavConduit)

1) Core Conduit class

Good
•	Centralizes success/failure/progress reporting.
•	Analytics hooks are clean.

Issues
•	scope = CoroutineScope(SupervisorJob() + Dispatchers.Main) inside a conduit is risky:
•	you’re doing analytics calls on Main
•	Conduit is created during upload in a background job
•	better: use Dispatchers.Default/IO for analytics submission, or just call analytics synchronously if it’s non-blocking.
•	Cancellation:
•	mCancelled boolean is not enough if network calls are blocking.
•	You must propagate cancellation to OkHttp calls / Sardine requests.

Best practice
•	Have Conduit expose cancel() that cancels real underlying calls:
•	for OkHttp: keep Call reference and call call.cancel()
•	for Sardine: ensure it checks continueUpload() frequently (you already do) — that’s good.

2) WebDavConduit

Good
•	Sardine listener uses continueUpload() and progress reporting—nice.
•	Chunking approach is pragmatic.

Bug / risk
•	Chunk loop:

offset = total + 1

That looks like an off-by-one. Usually next offset should be total, not total + 1. Otherwise you skip a byte each chunk boundary.
(If total is “exclusive end”, then naming is confusing; re-check carefully.)

More concerns
•	createFolders calls createDirectory sequentially; OK.
•	uploadMetadata runs before file upload. If file upload fails, you’ll have meta without file. Maybe OK, but consider:
•	upload file first, then meta (or upload meta at the end)
•	or clean up on failure if server supports it

3) IaConduit

Good
•	Upload content synchronously so progress can be tracked.
•	Metadata async is fine.

Concerns
•	You mark jobSucceeded() immediately after firing async metadata upload. That means:
•	UI says uploaded, DB status becomes Uploaded
•	but metadata could fail later → you call jobFailed(e) from OkHttp callback and mark media Error after it was set Uploaded (race)
•	Fix options:
•	Make metadata upload part of the “success” contract (await it), or
•	Mark “UploadedContent” vs “UploadedFully”, or
•	Treat metadata failure as non-fatal and don’t set status Error (record separately).

Also: mMedia.serverUrl is used as identifier; ensure it’s stable and not overwritten by later retry in weird states.

⸻

Concrete recommendations (highest impact first)

A) Make JobService correct & safe
•	Add service-owned coroutine job + cancel it
•	Reset flags at start:
•	mKeepUploading = true
•	mRunning = false should not be trusted; prefer Mutex/DB lock
•	Catch Throwable per-media in UploadService for consistency
•	Don’t schedule again inside shouldUpload()
•	Return correct boolean from onStopJob()

B) Scheduling & DI cleanup
•	Replace companion startUploadService() with UploadJobScheduler injected into ViewModel/use-case
•	Remove GlobalContext.get().get<Context>() usage
•	Remove injecting Context into JobService

C) Conduit correctness
•	Ensure cancellation cancels real network calls
•	Fix IA “success before metadata completes” race
•	Recheck WebDAV chunk offset logic

⸻

Answers to your direct questions (short)
•	How many times onStartJob called? Potentially many times; every schedule + constraint satisfied, plus reschedules after stops.
•	Duplicate instances running? Not supposed to run same jobId concurrently, but you can still get overlap if you reschedule aggressively or if old work doesn’t cancel correctly.
•	Will mRunning prevent duplicates? Only within a single instance; not process-safe and not thread-safe. Not enough as the main guard.

⸻

If you want, I can propose a clean “UploadCoordinator” split (JobService becomes 30 lines) and show exactly where to put:
•	DB lock / mutex
•	“wifi-only network constraint at schedule time”
•	success/failure mapping (including IA metadata phase)