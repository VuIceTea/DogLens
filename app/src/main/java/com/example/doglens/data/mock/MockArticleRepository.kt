package com.example.doglens.data.mock

import com.example.doglens.R
import com.example.doglens.data.model.Article
import com.example.doglens.data.model.ArticleCategory
import com.example.doglens.data.model.ArticleSection
import com.example.doglens.data.repository.ArticleRepository

class MockArticleRepository : ArticleRepository {
    private val categories = listOf(
        ArticleCategory("care", "Chăm sóc", "#DDF7E8", 1),
        ArticleCategory("nutrition", "Dinh dưỡng", "#FFF0D9", 2),
        ArticleCategory("training", "Huấn luyện", "#E3F1FF", 3),
        ArticleCategory("breeds", "Giống chó", "#F0E5FF", 4),
        ArticleCategory("health", "Sức khỏe", "#FFE3E8", 5)
    )

    private val articles = listOf(
        Article("care_coat", "care", "Cách chăm sóc lông chó đúng cách tại nhà", "Giữ bộ lông sạch, khỏe và hạn chế rụng lông bằng quy trình chăm sóc đơn giản.", localImageRes = R.drawable.group_hunting_golden, publishedAt = "08/10/2026", readMinutes = 5, viewCount = 1200),
        Article("puppy_food", "nutrition", "Chế độ dinh dưỡng cho chó theo từng độ tuổi", "Khẩu phần phù hợp cho chó con, chó trưởng thành và chó lớn tuổi.", localImageRes = R.drawable.topic_kid_friendly_golden, publishedAt = "07/10/2026", readMinutes = 6, viewCount = 980),
        Article("basic_training", "training", "Hướng dẫn huấn luyện chó cơ bản cho người mới", "Những khẩu lệnh và thói quen quan trọng khi bắt đầu huấn luyện.", localImageRes = R.drawable.corgi, publishedAt = "06/10/2026", readMinutes = 7, viewCount = 1500),
        Article("popular_breeds", "breeds", "Top 10 giống chó phổ biến nhất thế giới", "Khám phá các giống chó được yêu thích và đặc điểm nổi bật của chúng.", localImageRes = R.drawable.golden_retriever, publishedAt = "05/10/2026", readMinutes = 8, viewCount = 2100),
        Article("common_diseases", "health", "Những bệnh thường gặp ở chó và cách phòng ngừa", "Dấu hiệu cần chú ý và cách chăm sóc giúp chó luôn khỏe mạnh.", localImageRes = R.drawable.siberian_husky, publishedAt = "04/10/2026", readMinutes = 9, viewCount = 1750)
    )

    private val sections = articles.flatMap { article ->
        listOf(
            ArticleSection("${article.id}_1", article.id, 1, "Tại sao điều này quan trọng?", "Việc hiểu đúng nhu cầu của chó giúp bạn chăm sóc thú cưng khoa học hơn, hạn chế các vấn đề sức khỏe và xây dựng thói quen tốt mỗi ngày."),
            ArticleSection("${article.id}_2", article.id, 2, "Các bước thực hiện", "Hãy bắt đầu từ những thay đổi nhỏ, duy trì lịch trình ổn định và quan sát phản ứng của chó. Điều chỉnh phương pháp theo độ tuổi, giống chó và tình trạng sức khỏe."),
            ArticleSection("${article.id}_3", article.id, 3, "Những lưu ý quan trọng", "Luôn ưu tiên sự an toàn và thoải mái. Nếu xuất hiện dấu hiệu bất thường, hãy tham khảo ý kiến bác sĩ thú y hoặc chuyên gia có kinh nghiệm.")
        )
    }

    override suspend fun getViewedArticles(): List<Article> = articles
    override suspend fun getArticleById(id: String): Article? = articles.firstOrNull { it.id == id }
    override suspend fun getCategories(): List<ArticleCategory> = categories
    override suspend fun getSections(articleId: String): List<ArticleSection> =
        sections.filter { it.articleId == articleId }.sortedBy { it.sortIndex }
}
