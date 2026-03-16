CREATE TABLE IF NOT EXISTS users (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(255) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  role VARCHAR(50) NOT NULL DEFAULT 'PUBLIC',
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  last_login_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS artifacts (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(255) NOT NULL UNIQUE,
  description LONGTEXT NULL,
  image_url VARCHAR(512) NULL,
  thumbnail_url VARCHAR(512) NULL,
  tags VARCHAR(512) NULL,
  location VARCHAR(255) NULL,
  era VARCHAR(255) NULL,
  creator_id BIGINT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'APPROVED',
  is_deleted TINYINT(1) NOT NULL DEFAULT 0,
  vector_embedding_id VARCHAR(255) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_artifacts_status (status),
  INDEX idx_artifacts_deleted (is_deleted),
  INDEX idx_artifacts_era (era),
  INDEX idx_artifacts_creator (creator_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE artifacts ADD COLUMN IF NOT EXISTS is_deleted TINYINT(1) NOT NULL DEFAULT 0;
ALTER TABLE artifacts ADD INDEX IF NOT EXISTS idx_artifacts_deleted (is_deleted);

CREATE TABLE IF NOT EXISTS detection_tasks (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  image_url VARCHAR(512) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  detected_label VARCHAR(255) NULL,
  confidence FLOAT NULL,
  bbox TEXT NULL,
  raw_result TEXT NULL,
  team_id BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_detection_user (user_id),
  INDEX idx_detection_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS dialog_records (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  artifact_id BIGINT NULL,
  conversation_id BIGINT NULL,
  turn_id INT NOT NULL,
  user_query TEXT NOT NULL,
  ai_response TEXT NOT NULL,
  context_snapshot TEXT NULL,
  rag_sources TEXT NULL,
  team_id BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_dialog_user (user_id),
  INDEX idx_dialog_conversation (user_id, conversation_id),
  INDEX idx_dialog_artifact (artifact_id),
  INDEX idx_dialog_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS feedback (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NULL,
  type VARCHAR(50) NOT NULL DEFAULT 'GENERAL',
  text_content TEXT NOT NULL,
  rating INT NULL,
  screenshot_url VARCHAR(512) NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  handler_id BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_feedback_status (status),
  INDEX idx_feedback_type (type),
  INDEX idx_feedback_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS teams (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(255) NOT NULL UNIQUE,
  owner_id BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_teams_owner (owner_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS user_team_membership (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  team_id BIGINT NOT NULL,
  role VARCHAR(20) NOT NULL DEFAULT 'MEMBER',
  joined_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_team (user_id, team_id),
  INDEX idx_membership_team (team_id),
  INDEX idx_membership_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =====================================================
-- 种子数据：20件中国经典文物（APPROVED 状态）
-- =====================================================
INSERT IGNORE INTO artifacts (name, description, location, era, tags, status) VALUES
('后母戊鼎', '后母戊鼎，又称司母戊鼎，是中国商代晚期（约公元前14世纪至公元前11世纪）的青铜器，因鼎腹内壁铸有“后母戊”三字而得名。它是迄今为止出土的最重的青铜器，重达832.84千克，高133厘米，口长110厘米，口宽79厘米。1939年出土于河南省安阳市武官村，现藏于中国国家博物馆。鼎身四周饰有精美的云雷纹和饕餮纹，是商代青铜文化的巅峰之作，也是中国青铜时代的标志性文物。', '河南省安阳市武官村', '商代晚期', '青铜器,国宝', 'APPROVED'),
('四羊方尊', '四羊方尊是中国商代晚期的青铜礼器，1938年出土于湖南省宁乡市。尊高58.3厘米，重约34.5千克。器身方形，四角各铸一卷角羊头，羊角弯曲，栩栩如生。尊的肩部饰有四条蟠龙纹，腹部及足部满饰云雷纹。四羊方尊造型精美奇特，铸造工艺极为精湛，被认为是中国青铜铸造史上最杰出的作品之一。现藏于中国国家博物馆。', '湖南省宁乡市', '商代晚期', '青铜器,国宝', 'APPROVED'),
('曾侯乙编钟', '曾侯乙编钟是战国早期的大型礼乐器，1978年出土于湖北省随州市擂鼓墩曾侯乙墓。全套编钟共65件，分三层悬挂在铜木结构的钟架上，总重量约2567千克。编钟音域跨越五个八度，能演奏五声、六声乃至七声音阶的乐曲。每只钟可发出两个不同的音，音准精确。它是迄今发现的最完整、最大的一套青铜编钟，被誉为世界音乐史上的奇迹。现藏于湖北省博物馆。', '湖北省随州市', '战国早期', '青铜器,乐器', 'APPROVED'),
('越王勾践剑', '越王勾践剑是春秋晚期越国的青铜兵器，1965年出土于湖北省江陵县望山楚墓群。剑长55.7厘米，宽4.6厘米，剑身满饰菱形暗纹，正面铸有“越王鸠浅自作用剑”八字鸟篆铭文。出土时剑身完好如新，锋利无比，能轻松划开20余层纸张。历经两千多年不锈不钝，展现了春秋时期卓越的冶炼铸造技术。现藏于湖北省博物馆。', '湖北省江陵县', '春秋晚期', '青铜器,兵器', 'APPROVED'),
('清明上河图', '清明上河图是北宋画家张择端创作的风俗画长卷，宽24.8厘米，长528.7厘米。作品以精致的工笔记录了北宋都城汴京（今河南开封）的城市面貌和各阶层人民的生活状况。画面从城外的郊野开始，经过汴河两岸的繁华景象，最终进入城门内的街市。全卷共绘有814人、28条船、60多匹牲畜，生动地再现了12世纪中国城市的社会生活。现藏于北京故宫博物院。', '河南开封', '北宋', '书画,国宝', 'APPROVED'),
('翠玉白菜', '翠玉白菜是清代玉雕作品，高18.7厘米，宽9.1厘米，厚5.07厘米。以一块半白半绿的翡翠为原料，匠人巧妙利用玉石天然的颜色分布，将绿色部分雕为菜叶，白色部分雕为菜帮，菜叶上还停有两只昆虫——螽斯和蝗虫，寓意多子多孙。相传为光绪帝妃子瑾妃的嫁妆。翠玉白菜以其精巧的构思和高超的雕刻技艺闻名于世。现藏于台北故宫博物院。', '台北故宫博物院', '清代', '玉器', 'APPROVED'),
('马踏飞燕', '马踏飞燕，又称铜奔马，是东汉时期的青铜器，1969年出土于甘肃省武威市雷台汉墓。通高34.5厘米，长45厘米。铜马昂首嘶鸣，躯体壮实，四蹄腾空，右后蹄踏在一只展翅飞翔的燕子背上，飞燕回首惊视。造型生动，构思巧妙，完美地表现了骏马奔腾的动态美。1983年被定为中国旅游标志。现藏于甘肃省博物馆。', '甘肃省武威市', '东汉', '青铜器,国宝', 'APPROVED'),
('兵马俑', '秦始皇兵马俑是秦始皇陵的陪葬坑，位于陕西省西安市临潼区。1974年由当地农民打井时偶然发现。兵马俑坑已发掘三个，总面积约20000平方米，出土陶俑约8000件。陶俑平均身高1.8米，每个陶俑的脸型、神态、发型各不相同，栩栩如生。兵马俑按军阵排列，包括步兵、骑兵、战车兵等不同兵种，反映了秦代军事制度和雕塑艺术的高度成就。被誉为“世界第八大奇迹”。现为秦始皇帝陵博物院。', '陕西省西安市临潼区', '秦代', '雕塑,世界遗产', 'APPROVED'),
('三星堆青铜大立人像', '三星堆青铜大立人像是商代晚期的青铜铸像，1986年出土于四川省广汉市三星堆遗址二号祭祀坑。人像通高2.62米（含底座），重约180千克，是迄今发现的同时期最大的青铜人像。人像头戴高冠，身穿三层华服，脚戴足镯，双手呈环抱状。造型庄严神秘，被认为可能是古蜀国的巫师或神灵形象。三星堆出土的大量青铜器构成了独特的古蜀文明体系。现藏于三星堆博物馆。', '四川省广汉市', '商代晚期', '青铜器,古蜀文明', 'APPROVED'),
('汝窑天青釉洗', '汝窑天青釉洗是北宋汝窑瓷器的代表作品。汝窑是宋代五大名窑之首，窑址位于河南省汝州市（今宝丰县清凉寺）。汝窑瓷器以天青色釉闻名，釉面温润如玉，开片细密。传世汝窑瓷器极为稀少，全世界仅存约90件。天青釉洗口径约13厘米，器形规整，釉色青中泛蓝，釉面有细密的冰裂纹。“雨过天晴云破处，这般颜色做将来”，是形容汝窑天青色的经典诗句。台北故宫博物院藏有多件汝窑珍品。', '河南省汝州市', '北宋', '陶瓷,五大名窑', 'APPROVED'),
('敦煌莫高窟壁画', '敦煌莫高窟位于甘肃省敦煌市东南25公里的鸣沙山东麓，始建于前秦建元二年（366年），历经十六国至元代约千年的营建。现存洞窟735个，壁画4.5万平方米，泥质彩塑2415尊。壁画内容涵盖佛教故事、社会生活、山水风景等，色彩丰富，线条流畅，是中国古代艺术的巅峰之作。其中第17窟藏经洞曾藏有大量敦煌文献。莫高窟1987年被列入联合国教科文组织世界遗产名录。', '甘肃省敦煌市', '十六国至元代', '壁画,世界遗产', 'APPROVED'),
('大禹治水玉山', '大禹治水玉山是中国清代乾隆年间（约1787年完成）制作的大型玉雕作品，高224厘米，宽96厘米，重约5330千克，是世界上最大的玉雕作品之一。原料为新疆和田青玉，历经十余年从采石、运输到雕刻完成。玉山正面雕刻大禹率众开山治水的场景，人物众多，山势嶙峋，气势宏伟。背面刻有乾隆御制诗和制作年款。现藏于北京故宫博物院乐寿堂。', '北京故宫博物院', '清代', '玉器', 'APPROVED'),
('唐三彩骆驼载乐俑', '唐三彩骆驼载乐俑是唐代三彩釉陶艺术的杰出代表。高58.4厘米，骆驼背上驮着一个平台，平台上坐有7名乐手和1名歌者，乐手们手持不同的中西乐器演奏，歌者居中引吭高歌。这件作品生动地再现了盛唐时期丝绸之路上的文化交流景象。唐三彩以黄、绿、白三色为主，釉色流淌交融，绚丽多彩。该俑出土于陕西西安，现藏于中国国家博物馆。', '陕西西安', '唐代', '陶瓷,丝绸之路', 'APPROVED'),
('青花瓷鬼谷子下山图罐', '元青花鬼谷子下山图罐是元代景德镇窑烧制的青花瓷器，高27.5厘米，口径21.4厘米。罐身通体绘有“鬼谷子下山”的故事图案，描绘了战国时期纵横家鬼谷子坐在由虎豹拉的车中下山的场景，人物刻画生动，构图饱满。元代青花瓷器存世量极少，绘有完整人物故事的更是罕见。2005年在伦敦佳士得拍卖会上以约2.3亿元人民币成交，创下当时中国艺术品拍卖最高价。', '景德镇', '元代', '陶瓷,青花瓷', 'APPROVED'),
('毛公鼎', '毛公鼎是西周晚期的青铜器，清道光二十三年（1843年）出土于陕西省岐山县。鼎高53.8厘米，口径47.9厘米。毛公鼎最珍贵之处在于其内壁铸有铭文497字（一说500字），是迄今所见商周青铜器中铭文最长者。铭文记载了周宣王即位初年，对大臣毛公厝的册命和赏赐，是研究西周政治制度和社会历史的珍贵资料。现藏于台北故宫博物院。', '陕西省岐山县', '西周晚期', '青铜器', 'APPROVED'),
('洛神赋图', '洛神赋图是东晋画家顾恺之根据曹植的文学作品《洛神赋》而创作的绘画长卷。原作已佚，现存多幅宋代摹本。画卷以连续的方式描绘了曹植与洛水女神相遇、相恋、离别的完整故事。画中人物造型优美，笔法细劲流畅，衣带飘逸。“翩若惊鸿，婉若游龙”的洛神形象被刻画得超凡脱俗。洛神赋图是中国十大传世名画之一。北京故宫博物院藏有宋摹本。全卷长约572厘米，高约27厘米。', '北京故宫博物院', '东晋（宋摹本）', '书画', 'APPROVED'),
('良渚玉琮王', '良渚玉琮王是新石器时代良渚文化的代表性玉器，1986年出土于浙江省杭州市余杭区反山遗址12号墓。玉琮高8.9厘米，上射径17.1-17.6厘米，下射径16.5-17.5厘米，重约6.5千克。琮体四面各有两组“神人兽面纹”——上部为头戴羽冠的神人，下部为圆眼阔嘴的兽面。这种纹饰被认为是良渚先民崇拜的神徽。良渚文化距今约5300—4300年，其精美的玉器反映了高度发达的史前文明。现藏于浙江省博物馆。', '浙江省杭州市余杭区', '新石器时代', '玉器,良渚文化', 'APPROVED'),
('长信宫灯', '长信宫灯是西汉时期的青铜鎏金灯具，1968年出土于河北省满城县中山靖王刘胜之妻窦绾墓。灯高48厘米，通体鎏金。造型为一位跪坐的宫女，左手托灯盘，右手提灯罩，右臂中空。燃灯时烟气通过右臂进入中空的体内，防止空气污染。灯盘和灯罩可转动开合，以调节光照方向和亮度。长信宫灯集功能性和艺术性于一体，被誉为“中华第一灯”。现藏于河北博物院。', '河北省满城县', '西汉', '青铜器,灯具', 'APPROVED'),
('千里江山图', '千里江山图是北宋画家王希孟创作的青绿山水画长卷，绢本设色，纵51.5厘米，横1191.5厘米。画面以矿物颜料石青、石绿为主色调，描绘了连绵的群山、浩渺的江河、点缀其中的渔村野市、水榭亭台等景致。全卷构图大气磅礴，色彩鲜明，层次丰富。王希孟创作此画时年仅18岁，不久后英年早逝。千里江山图是中国青绿山水画的巅峰之作，现藏于北京故宫博物院。', '北京故宫博物院', '北宋', '书画,国宝', 'APPROVED'),
('金缕玉衣', '金缕玉衣是汉代皇帝和高级贵族使用的丧葬殓服，由玉片用金丝编缀而成。1968年出土于河北省满城县中山靖王刘胜墓的金缕玉衣是目前发现最完整的一件。全衣由2498片大小不等的玉片组成，用纯金丝约1100克编缀而成，分为头罩、上衣、袖套、手套、裤腿和鞋六部分。玉衣全长188厘米。汉代人认为玉能保护尸体不腐，因此以玉衣殓葬。金缕玉衣是汉代最高等级的葬具，体现了当时高超的玉器加工工艺。现藏于河北博物院。', '河北省满城县', '西汉', '玉器,丧葬', 'APPROVED');
