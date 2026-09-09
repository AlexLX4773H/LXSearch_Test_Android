import glob
import os
import re
import csv
import socket

# ── Regex patterns (from Utility_functions.py) ──────────────────────────────
pattern_square = r"(\[([^\]]+)\])"
pattern_circle = r"(\(([^\)]+)\))"
pattren_curly = r"({([^}]+)})"
pattren_equal = r"(=([^=]+)=)"

# ── Constants (from Utility_functions.py) ────────────────────────────────────
list_csv_delimiter = '╥'

list_csv_headers = ["Name","Folder","Extracted Name","Circle List","Curly List","Equal List","Author List","Tag List","Main Titles","Main Titles Pressed","Name Pressed"] #11

ARCHIVE_EXTENSIONS = {'.cbz', '.zip', '.rar', '.cbr', '.7z', '.cb7', '.tar', '.cbt', '.gz', '.xz'}
IMAGE_EXTENSIONS = {'.png', '.jpg', '.jpeg', '.webp', '.gif', '.bmp', '.tiff', '.avif'}

read_root_dir_for_folders = [
'/storage/emulated/0/Vere2/TempD',
'/storage/emulated/0/Vere2/TempT',
'/storage/emulated/0/Vere2/Vere/NewFolder',
'/storage/emulated/0/Vere2/Vere/Mihon/downloads/NineHentai (EN)',
'/storage/emulated/0/Vere2/Vere/Mihon/downloads/NHentai (EN)',
'/storage/emulated/0/Vere2/Vere/Mihon/downloads/nHentai.com (unoriginal) (EN)',
'/storage/emulated/0/Vere2/Vere/Mihon/downloads/E-Hentai (EN)',
'/storage/emulated/0/Vere2/Vere/Mihon/downloads/Hennojin (EN)',
'/storage/emulated/0/Vere2/Vere/Mihon/downloads/HentaiHand (ALL)',
'/storage/emulated/0/Vere2/Vere/Mihon/downloads/HentaiHand (EN)',
'/storage/emulated/0/Vere2/Vere/Mihon/downloads/AsmHentai (EN)',
'storage/emulated/0/Vere2/Vere/Mihon/downloads/NHentai.xxx (EN)']

read_root_dir_for_files = ['/storage/emulated/0/Vere2/TempT',
'/storage/emulated/0/Vere2/Vere/NewFolder/Files']

# ── SMB Configuration ────────────────────────────────────────────────────────
SHARE_PATH = r"\\192.168.88.234\Share2sgb"
USERNAME = "alex"
PASSWORD = "aaaaaaaa"
smb_read_root_dir_for_folders = [
    r"\\192.168.88.234\Share2sgb\Manga CBZ\Doujinshi\Archived"
]



# ── Helper functions (from Utility_functions.py) ─────────────────────────────

def check_re(string_to_check, re_list):
    for ele_1 in re_list:
        if re.fullmatch(ele_1, string_to_check.lower()):
            return True
    return False

def remove_list_from_string(string_to_remove, list_to_remove):
    for tmpr in list_to_remove:
        string_to_remove = string_to_remove.replace(tmpr, '')
    return string_to_remove.strip()

def string_press(input_string):
    return re.sub('[^A-Za-z0-9]+', '', input_string).lower()

def string_press_list(input_string_list):
    return [string_press(item) for item in input_string_list]

def extract_brackets(string_to_check):
    find_squares = re.findall(pattern_square, string_to_check)
    find_squares_remove = [i[0] for i in find_squares]
    find_squares_list = [i[1].strip() for i in find_squares]
    after_remove_square = remove_list_from_string(string_to_check, find_squares_remove)

    find_circle = re.findall(pattern_circle, after_remove_square)
    find_circle_remove = [i[0] for i in find_circle]
    find_circle_list = [i[1].strip() for i in find_circle]
    after_remove_circle = remove_list_from_string(after_remove_square, find_circle_remove)

    find_curly = re.findall(pattren_curly, after_remove_circle)
    find_curly_remove = [i[0] for i in find_curly]
    find_curly_list = [i[1].strip() for i in find_curly]
    after_remove_curly = remove_list_from_string(after_remove_circle, find_curly_remove)

    find_equal_list = []

    if after_remove_curly.count('=')%2 == 0:
        find_equal = re.findall(pattren_equal, after_remove_curly)
        find_equal_remove = [i[0] for i in find_equal]
        find_equal_list = [i[1].strip() for i in find_equal]
        after_remove_equal = remove_list_from_string(after_remove_curly, find_equal_remove)
        final_string = after_remove_equal
    else:
        final_string = after_remove_curly

    if final_string.isspace() or len(final_string) == 0:
        return string_to_check, find_circle_list, find_curly_list, find_equal_list, [], [], [], [], string_press(string_to_check)

    main_titles = final_string.split('_')
    main_titles = [item.strip() for item in main_titles]
    main_titles_pressed = string_press_list(main_titles)

    left_string = string_to_check.split(final_string)[0].strip()
    left_string_squares = re.findall(pattern_square, left_string)
    left_string_list = [i[1] for i in left_string_squares]

    left_author_list = [item for item in find_squares_list if item in left_string_list]
    right_tags_list = [item for item in find_squares_list if item not in left_string_list]

    return final_string, find_circle_list, find_curly_list, find_equal_list, left_author_list, right_tags_list, main_titles, main_titles_pressed, string_press(string_to_check)

def extract_smb_host(path):
    m = re.match(r'^[\\/]{2}([^\\/]+)', path)
    return m.group(1) if m else None

def is_smb_reachable(host, port=445, timeout=2.0):
    try:
        with socket.create_connection((host, port), timeout=timeout):
            return True
    except (socket.timeout, OSError):
        return False

def scan_smb_items(locations, share_path, username, password):
    if not locations:
        return [], []
    host = extract_smb_host(share_path) or extract_smb_host(locations[0])
    if not host:
        print("[SMB] No valid SMB host found in share path.")
        return [], []

    print(f"[SMB] Checking reachability of {host}...")
    if not is_smb_reachable(host, timeout=2.0):
        print(f"[SMB] Cannot reach SMB server at {host}. Skipping SMB scan.")
        return [], []

    print(f"[SMB] Server {host} is reachable. Connecting...")
    use_smbclient = False
    try:
        import smbclient
        smbclient.register_session(host, username=username, password=password)
        use_smbclient = True
    except Exception as e:
        print(f"[SMB] smbclient session registration failed ({e}), falling back to standard os.walk")

    found_folders = []
    found_files = []
    for loc in locations:
        print(f"[SMB] Scanning: {loc}")
        try:
            if use_smbclient:
                import smbclient
                if not smbclient.path.exists(loc):
                    print(f"[SMB] Path does not exist: {loc}")
                    continue
                for root, dirs, files in smbclient.walk(loc):
                    for d in dirs:
                        found_folders.append(os.path.join(root, d))
                    for f in files:
                        ext = os.path.splitext(f)[1].lower()
                        if ext in ARCHIVE_EXTENSIONS and ext not in IMAGE_EXTENSIONS:
                            found_files.append(os.path.join(root, f))
            else:
                if not os.path.exists(loc):
                    print(f"[SMB] Path does not exist: {loc}")
                    continue
                for root, dirs, files in os.walk(loc):
                    for d in dirs:
                        found_folders.append(os.path.join(root, d))
                    for f in files:
                        ext = os.path.splitext(f)[1].lower()
                        if ext in ARCHIVE_EXTENSIONS and ext not in IMAGE_EXTENSIONS:
                            found_files.append(os.path.join(root, f))
        except Exception as err:
            print(f"[SMB] Error scanning {loc}: {err}")
            continue

    return found_folders, found_files

def scan_smb_folders(locations, share_path, username, password):
    folders, _ = scan_smb_items(locations, share_path, username, password)
    return folders

# ── Main logic (from Run_Create_file_list.py) ────────────────────────────────

count = 0
temp_string = ""
final_list = [list_csv_headers]

folder_name_input = "input"
exclude_chapter_re = []
exclude_file_path = os.path.join(folder_name_input, "exclude_folders_chapter_re.txt")
if os.path.exists(exclude_file_path):
    with open(exclude_file_path, "r", encoding="utf-8") as f:
        for line in f:
            exclude_chapter_re.append(line.strip())

folder_name_output = "output"
os.makedirs(folder_name_output, exist_ok=True)

def process_folder(filename):
    global count, temp_string
    mystring = os.path.basename(filename)
    mystring = mystring.lower().strip()
    first = re.sub('[^A-Za-z0-9]+', '', mystring)
    if check_re(first, exclude_chapter_re) or check_re(mystring, exclude_chapter_re):
        return None
    second = str(filename)
    both = first + " ::: " + second + "\n"
    temp_string += both
    count += 1
    print(first, ' - ', count)

    mystring2 = os.path.basename(filename)
    if mystring2.isspace() or len(mystring2) == 0:
        return None
    xyz = extract_brackets(mystring2)
    final_xyz = [mystring2, second] + list(xyz)
def process_file(filename):
    global count, temp_string
    mystring = os.path.basename(filename)
    mystring = mystring.lower().strip()
    first = re.sub('[^A-Za-z0-9]+', '', mystring)
    name_no_ext = os.path.splitext(mystring)[0]
    first_no_ext = re.sub('[^A-Za-z0-9]+', '', name_no_ext)
    if (check_re(first, exclude_chapter_re) or
        check_re(mystring, exclude_chapter_re) or
        check_re(name_no_ext, exclude_chapter_re) or
        check_re(first_no_ext, exclude_chapter_re)):
        return None
    second = str(filename)
    both = first + " ::: " + second + "\n"
    temp_string += both
    count += 1
    print(first, ' - ', count)

    mystring2 = os.path.basename(filename)
    if mystring2.isspace() or len(mystring2) == 0:
        return None
    xyz = extract_brackets(mystring2)
    final_xyz = [mystring2, second] + list(xyz)
    return final_xyz

for root_dir in read_root_dir_for_folders:
    for filename in glob.iglob(root_dir + f'**{os.sep}**', recursive=True):
        if os.path.isdir(filename):
            res = process_folder(filename)
            if res:
                final_list.append(res) #11 fields

# ── Scan SMB directories for folders and archive files ───────────────────────
if smb_read_root_dir_for_folders:
    smb_folders, smb_files = scan_smb_items(smb_read_root_dir_for_folders, SHARE_PATH, USERNAME, PASSWORD)
    for filename in smb_folders:
        res = process_folder(filename)
        if res:
            final_list.append(res) #11 fields
    for filename in smb_files:
        res = process_file(filename)
        if res:
            final_list.append(res) #11 fields

for root_dir in read_root_dir_for_files:
    for filename in glob.iglob(root_dir + f'**{os.sep}**', recursive=True):
        if os.path.isfile(filename):
            res = process_file(filename)
            if res:
                final_list.append(res) #11 fields

f = open(os.path.join(folder_name_output, "list.txt"), "w", newline="", encoding="utf-8")
f.write(temp_string)
f.close()

f1 = open(os.path.join(folder_name_output, "count.txt"), "w",  newline="", encoding="utf-8")
f1.write(str(count))
f1.close()

with open(os.path.join(folder_name_output, "filename_list.csv"), "w", newline="", encoding="utf-8") as csvfile:
    csv_writer = csv.writer(csvfile, delimiter = list_csv_delimiter) #alt 1234
    # Write each row of the list to the CSV file
    csv_writer.writerows(final_list)
