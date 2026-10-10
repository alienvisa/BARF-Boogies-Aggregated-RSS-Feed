# WorkManager creates worker instances by class name and constructor.
-keep class app.localbrief.RefreshWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
