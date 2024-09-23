package blackorbs.dev.jetfiledownloader

class FakeApp: MainApp() {
    override val appModule: BaseAppModule by lazy {
        FakeAppModule(this)
    }
}