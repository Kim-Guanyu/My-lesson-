$ErrorActionPreference = 'Stop'

Add-Type -AssemblyName System.Drawing

$root = 'C:\Users\xiaoyuzi\Desktop\文件\my-lesson图片'
$dirs = @{
    banner = Join-Path $root 'banner'
    course = Join-Path $root 'course-cover'
    episode = Join-Path $root 'episode-video-cover'
    avatar = Join-Path $root 'avatar'
}

foreach ($dir in $dirs.Values) {
    if (-not (Test-Path $dir)) {
        New-Item -ItemType Directory -Path $dir -Force | Out-Null
    }
}

function New-Brush {
    param(
        [string]$Hex
    )
    return New-Object System.Drawing.SolidBrush ([System.Drawing.ColorTranslator]::FromHtml($Hex))
}

function Get-Palette {
    param(
        [int]$Seed
    )

    $palettes = @(
        @('#0F172A', '#2563EB', '#38BDF8', '#F8FAFC'),
        @('#111827', '#7C3AED', '#C084FC', '#F9FAFB'),
        @('#052E16', '#16A34A', '#4ADE80', '#F0FDF4'),
        @('#3F0D12', '#E11D48', '#FB7185', '#FFF1F2'),
        @('#1E1B4B', '#4F46E5', '#818CF8', '#EEF2FF'),
        @('#3B0764', '#9333EA', '#D8B4FE', '#FAF5FF'),
        @('#082F49', '#0284C7', '#7DD3FC', '#F0F9FF'),
        @('#422006', '#F59E0B', '#FCD34D', '#FFFBEB')
    )

    return $palettes[$Seed % $palettes.Count]
}

function Wrap-Text {
    param(
        [string]$Text,
        [int]$MaxCharsPerLine = 14,
        [int]$MaxLines = 4
    )

    if ([string]::IsNullOrWhiteSpace($Text)) {
        return @('')
    }

    $result = New-Object System.Collections.Generic.List[string]
    $current = ''
    foreach ($char in $Text.ToCharArray()) {
        if ($current.Length -ge $MaxCharsPerLine) {
            $result.Add($current)
            $current = ''
            if ($result.Count -ge $MaxLines) { break }
        }
        $current += $char
    }
    if ($result.Count -lt $MaxLines -and $current.Length -gt 0) {
        $result.Add($current)
    }
    if ($result.Count -eq $MaxLines -and ($Text.Length -gt ($MaxCharsPerLine * $MaxLines))) {
        $last = $result[$result.Count - 1]
        if ($last.Length -gt 2) {
            $result[$result.Count - 1] = $last.Substring(0, $last.Length - 2) + '..'
        }
    }
    return $result
}

function Save-Png {
    param(
        [System.Drawing.Bitmap]$Bitmap,
        [string]$Path
    )

    $Bitmap.Save($Path, [System.Drawing.Imaging.ImageFormat]::Png)
    $Bitmap.Dispose()
}

function New-BannerImage {
    param(
        [int]$Id,
        [string]$Headline,
        [string]$Subline,
        [string]$FileName
    )

    $palette = Get-Palette -Seed $Id
    $bmp = New-Object System.Drawing.Bitmap 1600, 600
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = 'AntiAlias'
    $g.TextRenderingHint = 'AntiAliasGridFit'
    $rect = New-Object System.Drawing.Rectangle 0, 0, 1600, 600
    $bg = New-Object System.Drawing.Drawing2D.LinearGradientBrush $rect, ([System.Drawing.ColorTranslator]::FromHtml($palette[0])), ([System.Drawing.ColorTranslator]::FromHtml($palette[1])), 20
    $g.FillRectangle($bg, $rect)
    $g.FillEllipse((New-Brush $palette[2]), 1120, -120, 520, 520)
    $g.FillEllipse((New-Brush '#FFFFFF22'), 1050, 250, 420, 420)
    $g.FillRectangle((New-Brush '#FFFFFF18'), 80, 70, 820, 78)
    $g.FillRectangle((New-Brush '#FFFFFF10'), 80, 170, 1100, 280)

    $titleFont = New-Object System.Drawing.Font 'Microsoft YaHei UI', 34, ([System.Drawing.FontStyle]::Bold)
    $heroFont = New-Object System.Drawing.Font 'Microsoft YaHei UI', 62, ([System.Drawing.FontStyle]::Bold)
    $bodyFont = New-Object System.Drawing.Font 'Microsoft YaHei UI', 22, ([System.Drawing.FontStyle]::Regular)
    $tagBrush = New-Brush '#FFFFFF'
    $bodyBrush = New-Brush '#E5F3FF'
    $accentBrush = New-Brush '#FDE68A'

    $g.DrawString("MyLesson 精选推荐", $titleFont, $tagBrush, 100, 86)
    $g.DrawString($Headline, $heroFont, $tagBrush, 100, 190)

    $subLines = Wrap-Text -Text $Subline -MaxCharsPerLine 26 -MaxLines 4
    $y = 300
    foreach ($line in $subLines) {
        $g.DrawString($line, $bodyFont, $bodyBrush, 102, $y)
        $y += 42
    }

    $g.FillRectangle($accentBrush, 100, 500, 240, 12)
    $g.DrawString("立即查看", $titleFont, $tagBrush, 1200, 470)

    $g.Dispose()
    Save-Png -Bitmap $bmp -Path (Join-Path $dirs.banner $FileName)
}

function New-CoverImage {
    param(
        [int]$Id,
        [string]$Title,
        [string]$Badge,
        [string]$FileName
    )

    $palette = Get-Palette -Seed $Id
    $bmp = New-Object System.Drawing.Bitmap 800, 1000
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = 'AntiAlias'
    $g.TextRenderingHint = 'AntiAliasGridFit'
    $rect = New-Object System.Drawing.Rectangle 0, 0, 800, 1000
    $bg = New-Object System.Drawing.Drawing2D.LinearGradientBrush $rect, ([System.Drawing.ColorTranslator]::FromHtml($palette[0])), ([System.Drawing.ColorTranslator]::FromHtml($palette[1])), 90
    $g.FillRectangle($bg, $rect)
    $g.FillEllipse((New-Brush '#FFFFFF10'), 440, -80, 340, 340)
    $g.FillEllipse((New-Brush '#FFFFFF12'), 520, 720, 220, 220)
    $g.FillRectangle((New-Brush '#FFFFFF12'), 60, 120, 250, 56)
    $g.FillRectangle((New-Brush '#00000028'), 60, 220, 680, 560)
    $g.FillRectangle((New-Brush $palette[2]), 60, 820, 140, 12)

    $smallFont = New-Object System.Drawing.Font 'Microsoft YaHei UI', 22, ([System.Drawing.FontStyle]::Bold)
    $titleFont = New-Object System.Drawing.Font 'Microsoft YaHei UI', 48, ([System.Drawing.FontStyle]::Bold)
    $footFont = New-Object System.Drawing.Font 'Microsoft YaHei UI', 26, ([System.Drawing.FontStyle]::Regular)
    $whiteBrush = New-Brush '#FFFFFF'
    $softBrush = New-Brush '#E5E7EB'

    $g.DrawString($Badge, $smallFont, $whiteBrush, 84, 132)
    $lines = Wrap-Text -Text $Title -MaxCharsPerLine 10 -MaxLines 5
    $y = 270
    foreach ($line in $lines) {
        $g.DrawString($line, $titleFont, $whiteBrush, 80, $y)
        $y += 86
    }
    $g.DrawString('MyLesson', $footFont, $softBrush, 82, 880)
    $g.DrawString('实战课程 / 系统学习 / 随学随练', $footFont, $softBrush, 82, 920)

    $g.Dispose()
    Save-Png -Bitmap $bmp -Path (Join-Path $dirs.course $FileName)
}

function New-EpisodeImage {
    param(
        [int]$Id,
        [string]$Title,
        [string]$FileName
    )

    $palette = Get-Palette -Seed $Id
    $bmp = New-Object System.Drawing.Bitmap 1280, 720
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = 'AntiAlias'
    $g.TextRenderingHint = 'AntiAliasGridFit'
    $rect = New-Object System.Drawing.Rectangle 0, 0, 1280, 720
    $bg = New-Object System.Drawing.Drawing2D.LinearGradientBrush $rect, ([System.Drawing.ColorTranslator]::FromHtml($palette[0])), ([System.Drawing.ColorTranslator]::FromHtml($palette[1])), 0
    $g.FillRectangle($bg, $rect)
    $g.FillEllipse((New-Brush '#FFFFFF15'), 840, -60, 420, 420)
    $g.FillRectangle((New-Brush '#00000040'), 70, 110, 820, 420)
    $g.FillRectangle((New-Brush $palette[2]), 70, 580, 220, 14)
    $playBrush = New-Brush '#FFFFFF'
    $points = [System.Drawing.Point[]]@(
        (New-Object System.Drawing.Point 980, 220),
        (New-Object System.Drawing.Point 980, 420),
        (New-Object System.Drawing.Point 1140, 320)
    )
    $g.FillPolygon($playBrush, $points)

    $badgeFont = New-Object System.Drawing.Font 'Microsoft YaHei UI', 24, ([System.Drawing.FontStyle]::Bold)
    $titleFont = New-Object System.Drawing.Font 'Microsoft YaHei UI', 40, ([System.Drawing.FontStyle]::Bold)
    $descFont = New-Object System.Drawing.Font 'Microsoft YaHei UI', 24, ([System.Drawing.FontStyle]::Regular)
    $whiteBrush = New-Brush '#FFFFFF'
    $softBrush = New-Brush '#E2E8F0'

    $g.DrawString('精品视频', $badgeFont, $whiteBrush, 96, 138)
    $lines = Wrap-Text -Text $Title -MaxCharsPerLine 14 -MaxLines 4
    $y = 220
    foreach ($line in $lines) {
        $g.DrawString($line, $titleFont, $whiteBrush, 96, $y)
        $y += 74
    }
    $g.DrawString('MyLesson 课程片段预览', $descFont, $softBrush, 96, 620)

    $g.Dispose()
    Save-Png -Bitmap $bmp -Path (Join-Path $dirs.episode $FileName)
}

function New-AvatarImage {
    param(
        [int]$Id,
        [string]$Title,
        [string]$FileName
    )

    $palette = Get-Palette -Seed $Id
    $bmp = New-Object System.Drawing.Bitmap 512, 512
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = 'AntiAlias'
    $g.TextRenderingHint = 'AntiAliasGridFit'
    $rect = New-Object System.Drawing.Rectangle 0, 0, 512, 512
    $bg = New-Object System.Drawing.Drawing2D.LinearGradientBrush $rect, ([System.Drawing.ColorTranslator]::FromHtml($palette[1])), ([System.Drawing.ColorTranslator]::FromHtml($palette[2])), 45
    $g.FillEllipse($bg, 0, 0, 512, 512)
    $g.FillEllipse((New-Brush '#FFFFFF22'), 70, 70, 372, 372)
    $g.FillRectangle((New-Brush '#00000020'), 110, 330, 290, 80)

    $labelFont = New-Object System.Drawing.Font 'Microsoft YaHei UI', 28, ([System.Drawing.FontStyle]::Bold)
    $titleFont = New-Object System.Drawing.Font 'Microsoft YaHei UI', 64, ([System.Drawing.FontStyle]::Bold)
    $whiteBrush = New-Brush '#FFFFFF'

    $g.DrawString('ADMIN', $labelFont, $whiteBrush, 176, 116)
    $g.DrawString($Title, $titleFont, $whiteBrush, 126, 230)

    $g.Dispose()
    Save-Png -Bitmap $bmp -Path (Join-Path $dirs.avatar $FileName)
}

$banners = @(
    @{ Id = 1; Headline = 'Python 零基础'; Subline = '入门到项目实战，快速打开编程世界的大门'; FileName = 'banner-1.png' },
    @{ Id = 2; Headline = 'Office 二级冲刺'; Subline = '高频考点拆解，刷题讲解与押题资料同步拿下'; FileName = 'banner-2.png' },
    @{ Id = 3; Headline = 'UI 设计进阶'; Subline = '设计软件实操加作品集打磨，适合转岗提升'; FileName = 'banner-3.png' },
    @{ Id = 4; Headline = '新媒体运营实战'; Subline = '内容制作与账号增长并行，适合副业和转型'; FileName = 'banner-4.png' },
    @{ Id = 5; Headline = '年度 VIP 会员'; Subline = '热门课程一站式学习，搭配导师规划与直播答疑'; FileName = 'banner-5.png' },
    @{ Id = 6; Headline = 'PS 图像处理'; Subline = '从工具认识到海报修图，新手也能快速上手'; FileName = 'banner-6.png' },
    @{ Id = 7; Headline = '求职面试直播'; Subline = '简历优化与面试技巧一次讲清，提升求职成功率'; FileName = 'banner-7.png' },
    @{ Id = 8; Headline = 'Java 高级实战'; Subline = 'SpringBoot 与微服务项目深度训练，面向就业'; FileName = 'banner-8.png' },
    @{ Id = 9; Headline = '邀请好友有礼'; Subline = '邀请越多奖励越多，课程券与 VIP 资格可叠加'; FileName = 'banner-9.png' },
    @{ Id = 10; Headline = '碎片化学习营'; Subline = '每天 15 分钟，让技能成长更轻松持续'; FileName = 'banner-10.png' }
)

$courses = @(
    @{ Id = 1; Title = 'JB1-1-新手村'; FileName = 'course-1.png' },
    @{ Id = 2; Title = 'JB1-2-基础启航'; FileName = 'course-2.png' },
    @{ Id = 3; Title = 'JB1-3-面向对象'; FileName = 'course-3.png' },
    @{ Id = 4; Title = 'JB1-4-高级进阶'; FileName = 'course-4.png' },
    @{ Id = 5; Title = 'JB1-5-数据结构'; FileName = 'course-5.png' },
    @{ Id = 6; Title = 'JB1-6-虚拟内存'; FileName = 'course-6.png' },
    @{ Id = 7; Title = 'JB1-7-并发编程'; FileName = 'course-7.png' },
    @{ Id = 8; Title = 'JB1-8-设计模式'; FileName = 'course-8.png' },
    @{ Id = 9; Title = 'JB1-9-网络编程'; FileName = 'course-9.png' },
    @{ Id = 10; Title = 'JB2-1-Linux'; FileName = 'course-10.png' },
    @{ Id = 11; Title = 'JB2-2-Docker'; FileName = 'course-11.png' },
    @{ Id = 12; Title = 'JB2-3-MySQL'; FileName = 'course-12.png' },
    @{ Id = 13; Title = 'JB2-4-JDBC'; FileName = 'course-13.png' },
    @{ Id = 14; Title = 'JB2-5-Tomcat'; FileName = 'course-14.png' },
    @{ Id = 15; Title = 'JB2-6-Servlet'; FileName = 'course-15.png' },
    @{ Id = 16; Title = 'JB2-7-HTML'; FileName = 'course-16.png' },
    @{ Id = 17; Title = 'JB2-8-CSS'; FileName = 'course-17.png' },
    @{ Id = 18; Title = 'JB2-9-JavaScript'; FileName = 'course-18.png' },
    @{ Id = 19; Title = 'JB3-1-MyBatis'; FileName = 'course-19.png' },
    @{ Id = 20; Title = 'JB3-2-Spring'; FileName = 'course-20.png' },
    @{ Id = 21; Title = 'JB3-3-SpringMVC'; FileName = 'course-21.png' },
    @{ Id = 22; Title = 'JB3-4-SpringBoot'; FileName = 'course-22.png' },
    @{ Id = 23; Title = 'JB3-5-Redis'; FileName = 'course-23.png' },
    @{ Id = 24; Title = 'JB3-6-Elasticsearch'; FileName = 'course-24.png' },
    @{ Id = 25; Title = 'JB3-7-MongoDB'; FileName = 'course-25.png' },
    @{ Id = 26; Title = 'JB3-8-Nginx'; FileName = 'course-26.png' },
    @{ Id = 27; Title = 'JB3-9-Vue'; FileName = 'course-27.png' },
    @{ Id = 28; Title = 'JB4-1-注册中心'; FileName = 'course-28.png' },
    @{ Id = 29; Title = 'JB4-2-远程调用'; FileName = 'course-29.png' },
    @{ Id = 30; Title = 'JB4-3-流量卫兵'; FileName = 'course-30.png' },
    @{ Id = 31; Title = 'JB4-4-服务网关'; FileName = 'course-31.png' },
    @{ Id = 32; Title = 'JB4-5-链路追踪'; FileName = 'course-32.png' },
    @{ Id = 33; Title = 'JB4-6-消息队列'; FileName = 'course-33.png' },
    @{ Id = 34; Title = 'JB4-7-事务机制'; FileName = 'course-34.png' },
    @{ Id = 35; Title = 'JB4-8-任务调度'; FileName = 'course-35.png' },
    @{ Id = 36; Title = 'JB4-9-人工智能'; FileName = 'course-36.png' }
)

$episodes = @(
    @{ Id = 1; Title = 'Java语言年龄分代'; FileName = 'episode-1.png' },
    @{ Id = 2; Title = 'Java核心技术特点'; FileName = 'episode-2.png' },
    @{ Id = 3; Title = 'Java学习路线分享'; FileName = 'episode-3.png' },
    @{ Id = 4; Title = 'Java项目开发流程'; FileName = 'episode-4.png' },
    @{ Id = 5; Title = 'Java学习技巧分享'; FileName = 'episode-5.png' },
    @{ Id = 6; Title = 'Java学习笔记工具'; FileName = 'episode-6.png' },
    @{ Id = 7; Title = 'Java学前环境准备'; FileName = 'episode-7.png' },
    @{ Id = 8; Title = 'Java开发环境搭建'; FileName = 'episode-8.png' },
    @{ Id = 9; Title = 'Java环境变量配置'; FileName = 'episode-9.png' },
    @{ Id = 10; Title = 'Java环境变量升级'; FileName = 'episode-10.png' },
    @{ Id = 11; Title = 'Java入门程序开发'; FileName = 'episode-11.png' },
    @{ Id = 12; Title = 'Java入门程序详解'; FileName = 'episode-12.png' },
    @{ Id = 13; Title = 'IDEA开发工具安装'; FileName = 'episode-13.png' },
    @{ Id = 14; Title = 'IDEA创建基础项目'; FileName = 'episode-14.png' },
    @{ Id = 15; Title = 'IDEA基础环境配置'; FileName = 'episode-15.png' },
    @{ Id = 16; Title = 'IDEA常用插件分享'; FileName = 'episode-16.png' },
    @{ Id = 17; Title = 'Maven项目管理工具'; FileName = 'episode-17.png' },
    @{ Id = 18; Title = 'Maven管理工具安装'; FileName = 'episode-18.png' },
    @{ Id = 19; Title = 'Maven本地仓库搭建'; FileName = 'episode-19.png' },
    @{ Id = 20; Title = 'Maven管理工具整合'; FileName = 'episode-20.png' },
    @{ Id = 21; Title = 'Maven父子项目创建'; FileName = 'episode-21.png' },
    @{ Id = 22; Title = 'Maven一键构建测试'; FileName = 'episode-22.png' },
    @{ Id = 23; Title = 'Junit测试的使用规范'; FileName = 'episode-23.png' },
    @{ Id = 24; Title = 'Junit依赖的生效范围'; FileName = 'episode-24.png' },
    @{ Id = 25; Title = 'Junit依赖的版本管理'; FileName = 'episode-25.png' },
    @{ Id = 26; Title = 'Junit依赖的真实引入'; FileName = 'episode-26.png' },
    @{ Id = 27; Title = '安装并整合Git服务端'; FileName = 'episode-27.png' },
    @{ Id = 28; Title = '注册并整合Git远程库'; FileName = 'episode-28.png' },
    @{ Id = 29; Title = '分享项目到Git远程库'; FileName = 'episode-29.png' },
    @{ Id = 30; Title = '邀请队友到Git项目组'; FileName = 'episode-30.png' },
    @{ Id = 31; Title = '克隆项目到Git本地库'; FileName = 'episode-31.png' },
    @{ Id = 32; Title = '推送代码到Git远程库'; FileName = 'episode-32.png' },
    @{ Id = 33; Title = '代码的基本注释'; FileName = 'episode-33.png' },
    @{ Id = 34; Title = '产品的文档注释'; FileName = 'episode-34.png' },
    @{ Id = 35; Title = '换行与同行输出'; FileName = 'episode-35.png' },
    @{ Id = 36; Title = '格式化输出语句'; FileName = 'episode-36.png' },
    @{ Id = 37; Title = '输出基础表达式'; FileName = 'episode-37.png' },
    @{ Id = 38; Title = '输出特殊的字符'; FileName = 'episode-38.png' },
    @{ Id = 39; Title = '先天与后天常量'; FileName = 'episode-39.png' },
    @{ Id = 40; Title = '特殊进制的常量'; FileName = 'episode-40.png' },
    @{ Id = 41; Title = '计算机计算原理'; FileName = 'episode-41.png' },
    @{ Id = 42; Title = '变量的声明方式'; FileName = 'episode-42.png' },
    @{ Id = 43; Title = '变量的赋值方式'; FileName = 'episode-43.png' },
    @{ Id = 44; Title = '变量的命名规范'; FileName = 'episode-44.png' },
    @{ Id = 45; Title = '代码的整洁之道'; FileName = 'episode-45.png' },
    @{ Id = 46; Title = '基本类型之整数'; FileName = 'episode-46.png' },
    @{ Id = 47; Title = '基本类型之浮点'; FileName = 'episode-47.png' },
    @{ Id = 48; Title = '基本类型之字符'; FileName = 'episode-48.png' },
    @{ Id = 49; Title = '基本类型之布尔'; FileName = 'episode-49.png' },
    @{ Id = 50; Title = '基本类型的转换'; FileName = 'episode-50.png' },
    @{ Id = 51; Title = '跨类型计算原则'; FileName = 'episode-51.png' },
    @{ Id = 52; Title = '计算结果的溢出'; FileName = 'episode-52.png' },
    @{ Id = 53; Title = '引用类型字符串'; FileName = 'episode-53.png' },
    @{ Id = 54; Title = '字符串编程接口'; FileName = 'episode-54.png' },
    @{ Id = 55; Title = '正则表达式专题'; FileName = 'episode-55.png' },
    @{ Id = 56; Title = '运行时数据区域'; FileName = 'episode-56.png' },
    @{ Id = 57; Title = '引用的比较方式'; FileName = 'episode-57.png' },
    @{ Id = 58; Title = '装箱和拆箱过程'; FileName = 'episode-58.png' },
    @{ Id = 59; Title = '数学相关运算符'; FileName = 'episode-59.png' },
    @{ Id = 60; Title = '赋值相关运算符'; FileName = 'episode-60.png' },
    @{ Id = 61; Title = '自我相关运算符'; FileName = 'episode-61.png' },
    @{ Id = 62; Title = '关系相关运算符'; FileName = 'episode-62.png' },
    @{ Id = 63; Title = '逻辑相关运算符'; FileName = 'episode-63.png' },
    @{ Id = 64; Title = '比特相关运算符'; FileName = 'episode-64.png' },
    @{ Id = 65; Title = '三目逻辑运算符'; FileName = 'episode-65.png' },
    @{ Id = 66; Title = '选择之范围匹配'; FileName = 'episode-66.png' },
    @{ Id = 67; Title = '选择之定值匹配'; FileName = 'episode-67.png' },
    @{ Id = 68; Title = '仅知结束的条件'; FileName = 'episode-68.png' },
    @{ Id = 69; Title = '仅知循环的次数'; FileName = 'episode-69.png' },
    @{ Id = 70; Title = '单维度数组结构'; FileName = 'episode-70.png' },
    @{ Id = 71; Title = '单维度数组遍历'; FileName = 'episode-71.png' },
    @{ Id = 72; Title = '多维度数组结构'; FileName = 'episode-72.png' },
    @{ Id = 73; Title = '多维度数组遍历'; FileName = 'episode-73.png' },
    @{ Id = 74; Title = '万物皆为对象'; FileName = 'episode-74.png' },
    @{ Id = 75; Title = '面向对象编程'; FileName = 'episode-75.png' },
    @{ Id = 76; Title = '抽象编程思想'; FileName = 'episode-76.png' },
    @{ Id = 77; Title = '封装最终产物'; FileName = 'episode-77.png' },
    @{ Id = 78; Title = '类的属性分类'; FileName = 'episode-78.png' },
    @{ Id = 79; Title = '类的方法分类'; FileName = 'episode-79.png' },
    @{ Id = 80; Title = '方法传参内容'; FileName = 'episode-80.png' },
    @{ Id = 81; Title = '方法递归调用'; FileName = 'episode-81.png' },
    @{ Id = 82; Title = '类的初始化块'; FileName = 'episode-82.png' },
    @{ Id = 83; Title = '继承基本原则'; FileName = 'episode-83.png' },
    @{ Id = 84; Title = '父子类间调用'; FileName = 'episode-84.png' },
    @{ Id = 85; Title = '多态具体实现'; FileName = 'episode-85.png' },
    @{ Id = 86; Title = '动态绑定机制'; FileName = 'episode-86.png' },
    @{ Id = 87; Title = '特殊之抽象类'; FileName = 'episode-87.png' },
    @{ Id = 88; Title = '特殊之接口类'; FileName = 'episode-88.png' },
    @{ Id = 89; Title = '特殊之枚举类'; FileName = 'episode-89.png' },
    @{ Id = 90; Title = '特殊之内部类'; FileName = 'episode-90.png' },
    @{ Id = 91; Title = '特殊之注解类'; FileName = 'episode-91.png' },
    @{ Id = 92; Title = '龙目岛工具类'; FileName = 'episode-92.png' },
    @{ Id = 93; Title = '拉姆达表达式'; FileName = 'episode-93.png' },
    @{ Id = 94; Title = '异常基本结构'; FileName = 'episode-94.png' },
    @{ Id = 95; Title = '异常自动关闭'; FileName = 'episode-95.png' },
    @{ Id = 96; Title = '异常结果缓存'; FileName = 'episode-96.png' },
    @{ Id = 97; Title = '异常抛出方案'; FileName = 'episode-97.png' },
    @{ Id = 98; Title = '异常重写原则'; FileName = 'episode-98.png' },
    @{ Id = 99; Title = '形参上的泛型'; FileName = 'episode-99.png' },
    @{ Id = 100; Title = '泛型强制限定'; FileName = 'episode-100.png' },
    @{ Id = 101; Title = '常用序列容器'; FileName = 'episode-101.png' },
    @{ Id = 102; Title = '常用集合容器'; FileName = 'episode-102.png' },
    @{ Id = 103; Title = '常用映射容器'; FileName = 'episode-103.png' },
    @{ Id = 104; Title = '迭代器遍历法'; FileName = 'episode-104.png' },
    @{ Id = 105; Title = '获取类的对象'; FileName = 'episode-105.png' },
    @{ Id = 106; Title = '反射构造方法'; FileName = 'episode-106.png' },
    @{ Id = 107; Title = '反射成员属性'; FileName = 'episode-107.png' },
    @{ Id = 108; Title = '反射成员方法'; FileName = 'episode-108.png' },
    @{ Id = 109; Title = '反射方法泛型'; FileName = 'episode-109.png' },
    @{ Id = 110; Title = '反射运行注解'; FileName = 'episode-110.png' },
    @{ Id = 111; Title = '动态编译代码'; FileName = 'episode-111.png' },
    @{ Id = 112; Title = '动态运行代码'; FileName = 'episode-112.png' },
    @{ Id = 113; Title = '流类之文件流'; FileName = 'episode-113.png' },
    @{ Id = 114; Title = '流类之缓冲流'; FileName = 'episode-114.png' },
    @{ Id = 115; Title = '流类之转换流'; FileName = 'episode-115.png' },
    @{ Id = 116; Title = '流类之打印流'; FileName = 'episode-116.png' },
    @{ Id = 117; Title = '流类之数据流'; FileName = 'episode-117.png' },
    @{ Id = 118; Title = '流类之对象流'; FileName = 'episode-118.png' },
    @{ Id = 119; Title = '数学运算工具'; FileName = 'episode-119.png' },
    @{ Id = 120; Title = '字符拼接工具'; FileName = 'episode-120.png' },
    @{ Id = 121; Title = '线性结构工具'; FileName = 'episode-121.png' },
    @{ Id = 122; Title = '日期时间工具'; FileName = 'episode-122.png' },
    @{ Id = 123; Title = '文件对象工具'; FileName = 'episode-123.png' },
    @{ Id = 124; Title = '空值处理工具'; FileName = 'episode-124.png' },
    @{ Id = 125; Title = '定时任务工具'; FileName = 'episode-125.png' },
    @{ Id = 126; Title = '流式编程工具'; FileName = 'episode-126.png' }
)

$avatars = @(
    @{ Id = 1; Title = 'A1'; FileName = 'user-1.png' },
    @{ Id = 2; Title = 'A2'; FileName = 'user-2.png' }
)

foreach ($banner in $banners) {
    New-BannerImage -Id $banner.Id -Headline $banner.Headline -Subline $banner.Subline -FileName $banner.FileName
}

foreach ($course in $courses) {
    New-CoverImage -Id $course.Id -Title $course.Title -Badge '精品课程' -FileName $course.FileName
}

foreach ($episode in $episodes) {
    New-EpisodeImage -Id $episode.Id -Title $episode.Title -FileName $episode.FileName
}

foreach ($avatar in $avatars) {
    New-AvatarImage -Id $avatar.Id -Title $avatar.Title -FileName $avatar.FileName
}

Write-Host "图片生成完成，输出目录：$root"
