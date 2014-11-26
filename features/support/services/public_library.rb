module KnowsAboutLibraryAPI
  def get_library
    http_get :consumer_api, "/my/library", "Accept" => "application/vnd.blinkbox.books.v2+json"
    @response_data = parse_last_api_response
  end
end

World(KnowsAboutLibraryAPI)
