from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", extra="ignore")

    masfoot_api_key: str = ""
    football_data_api_key: str = ""
    api_football_key: str = ""
    broadcast_catalog_url: str = ""
    broadcast_catalog_token: str = ""
    sportium_api_url: str = ""
    sportium_api_token: str = ""
    default_country: str = "ES"
    sync_live_interval: int = 60
    sync_fixtures_interval: int = 21600


settings = Settings()
