module KnowsAboutAdminLibraryAPI
  def add_book_to_library(user_id, payload)
    http_post :admin_api, "admin/users/#{user_id}/library", payload, "Content-Type" => "application/vnd.blinkbox.books.v2+json"
    @response_data = parse_last_api_response
  end
end

World(KnowsAboutAdminLibraryAPI)
